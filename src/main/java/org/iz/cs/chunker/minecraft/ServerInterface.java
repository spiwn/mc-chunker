/*
 * Copyright 2020 Ivan Zhivkov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iz.cs.chunker.minecraft;

import static org.iz.cs.chunker.io.ConsolePrinter.println;
import static org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.iz.cs.chunker.Chunker;
import org.iz.cs.chunker.Configuration;
import org.iz.cs.chunker.GenerationProgress;
import org.iz.cs.chunker.JarClassLoader;
import org.iz.cs.chunker.Mapping;
import org.iz.cs.chunker.io.InputHandler;
import org.iz.cs.chunker.io.OutputHandler;
import org.iz.cs.chunker.minecraft.impl.GenerateChunk.GenerateChunkArguments;

import com.google.gson.Gson;

public class ServerInterface {

//  private static String CHUNK_ACCESS_CN = "net.minecraft.world.level.chunk.ChunkAccess";
//  private static String IS_UNSAVED_M = "isUnsaved";
//  private static String SET_UNSAVED_M = "setUnsaved";
//  private static String GET_STATUS_M = "getStatus";
//  private static String LOCATION_M = "location";
//  private static String RESOURCE_LOCATION_CN = "net.minecraft.resources.ResourceLocation";
//  private static String GET_PATH_M = "getPath";

    private static final String SERVER_THREAD_NAME = "Server thread";

    private volatile boolean serverRunning = false;
    private volatile boolean shuttingDown = false;
    private volatile int lastX = 0;
    private volatile int lastZ = 0;

    private InputHandler inputHandler;
    private JarClassLoader loader;
    private BehaviorManager bm;
    private ThreadGroup minecraftThreadGroup;

    private Thread serverThread = null;

    private ServerInterface(
            JarClassLoader loader,
            Mapping mapping,
            String versionId,
            InputHandler inputHandler)
                    throws Exception {
        this.loader = loader;
        this.inputHandler = inputHandler;
        this.bm = new BehaviorManager(versionId, new ClassCache(mapping, loader), mapping, this);

        String dedicatedServerClassName = (String) GET_DEDICATED_SERVER_CLASS_NAME.apply(null, bm);

        loader.setClassToDecorate(dedicatedServerClassName);

        // Fail early;
        bm.checkMappings();
        bm.checkClasses();
    }

    public void startServer(String[] args) throws Exception {
        println("Starting Minecraft server");
        String main = loader.getMain();

        Class<?> mainClazz = loader.loadClass(main);

        Method mainMethod = mainClazz.getDeclaredMethod("main", String[].class);
        minecraftThreadGroup = new ThreadGroup("Minecraft threads");
        Thread t = new Thread(minecraftThreadGroup, new ServerStarter(args, mainMethod), "Server starter");
        t.setContextClassLoader(loader);
        t.start();
        long seconds = 100L;
        t.join(seconds * 1000L);
        if (t.isAlive()) {
            serverRunning = false;
            throw new IllegalStateException("Server did not start in " + seconds + " seconds");
        }
        this.serverThread = getServerThread();
        t = new Thread(new ServerShutdownListener(), "Server Shutdown Listener");
        t.setDaemon(true);
        t.start();
        serverRunning = true;

        println("Minecreaft server started");
    }

    public Boolean isServerReady() {
        return (Boolean) IS_SERVER_READY.apply(null, bm);
    }

    public synchronized boolean generateChunks(
            String dimension,
            int x1, int x2,
            int z1, int z2,
            GenerationProgress oldProgress) {
        if (shuttingDown) {
            return false;
        }

        int total = (x2 - x1 + 1) * (z2 - z1 + 1);
        int counter = 0;

        int step;
        float percentIncrement;

        println("Starting generating chunks for area (" + x1 + ", " + z1 + ") (" + x2 + ", " + z2 + "), "
                + "dimension " + dimension + ". "
                + "Area contains " + total + " total chunks" );

        int xStart = x1;
        int zStart = z1;
        if (oldProgress != null && oldProgress.getX() != null) {
            xStart = oldProgress.getX();
            zStart = oldProgress.getZ();
            if (zStart == z2) {
                xStart++;
                zStart = z1;
            }
            total = (x2 - xStart + 1) * (z2 - z1) + (z2 - zStart);
            println("Resuming from (" + xStart + ", " + zStart + "). "
                    + "Remaining chunks: " + total);
        }

        if (total < 200) {
            step = 1;
            percentIncrement = 100f / (float) total;
        } else {
            step = total / 200;
            percentIncrement = 0.5f;
        }
        float progress = 0;

        GenerateChunkArguments generateChunkParameters = new GenerateChunkArguments(dimension, 0, 0);

        long limit = 0;
        long currentTime = 0;
        if (Configuration.maxGenerationRate != null) {
            println("Limiting chunk generation to " + Configuration.maxGenerationRate.toPlainString() + " per second");
            limit = new BigDecimal(1000).divide(Configuration.maxGenerationRate, 0, RoundingMode.DOWN).longValue();
        }
        long goal = 0L;

        long start = System.currentTimeMillis();

        outer:
        for (int i = xStart; i <= x2; i++) {
            for (int j = zStart; j <= z2; j++) {
                generateChunkParameters.setX(x1);
                generateChunkParameters.setZ(z1);

                if (this.serverThread != null && this.serverThread.isAlive()) {
                    GENERATE_CHUNK.apply(generateChunkParameters, bm);
                } else {
                    break outer;
                }

                if (Configuration.saveProgress) {
                    this.lastX = i;
                    this.lastZ = j;
                }

                if (this.shuttingDown) {
                    break outer;
                }

                if (limit > 0) {
                    currentTime = System.currentTimeMillis();
                    goal += limit;
                    long diff = goal - (currentTime - start);
                    if (diff > 0) {
                        try {
                            Thread.sleep(diff);
                        } catch (InterruptedException e) {
                            println("Chunk generation interrupted");
                        }
                    }
                }

                if (++counter == step) {
                    counter = 0;
                    progress += percentIncrement;
                    long time = System.currentTimeMillis() - start;
                    println("Progress: " + progress +
                            "% Elapsed: " + (float) time/ 1000 + "s "
                            + "Remaining estimate: " + ((time * (100 / progress) - time) / 1000) + "s");
                }
            }
            zStart = z1;
        }
        println("Done generating chunk in dimension " + dimension);
        return true;
    }

    public int getLastX() {
        return lastX;
    }

    public int getLastZ() {
        return lastZ;
    }

    public void scheduleShutdown() {
        if (serverRunning) {
            if (Configuration.saveProgress) {
                shuttingDown = true;
                return;
            }
        }
    }

    public void stopServer() {
        if (serverRunning) {

            println("Stopping server");
            inputHandler.enqueue("stop\n", StandardCharsets.UTF_8, true);

            if (this.serverThread != null) {
                try {
                    this.serverThread.join();
                } catch (InterruptedException e) {
                    Chunker.defaultErr.print("Interupted while waiting for server to stop.");
                    e.printStackTrace(Chunker.defaultErr);
                }
            }
        } else {
            println("Server seems to not be running. Skipping attempt to stop it");
        }
    }

    private Thread getServerThread() {
        Thread[] threads = new Thread[64];
        minecraftThreadGroup.enumerate(threads, false);
        for (Thread thread : threads) {
            if (SERVER_THREAD_NAME.equals(thread.getName())) {
                return thread;
            }
        }
        return null;
    }


    public boolean isServerRunning() {
        return (!shuttingDown)
                && this.serverRunning
                && this.serverThread != null
                && this.serverThread.isAlive();
    }

    private void setServerRunning(boolean serverRunning) {
        this.serverRunning = serverRunning;
    }

    @SuppressWarnings("unchecked")
    private static String getServerVersionId(JarFile jarFile) {
        JarEntry versionEntry = jarFile.getJarEntry("version.json");
        if (versionEntry == null) {
            throw new IllegalArgumentException("Server jar does not have version.json. "
                    + "If it is a vanilla server jar, than the version is not supported.");
        }
        try (InputStream is = jarFile.getInputStream(versionEntry)) {
            Gson g = new Gson();
            Map<String, Object> m = (Map<String, Object>) g.fromJson(
                    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)), Map.class);

            return (String) m.get("id");
        } catch (IOException e) {
            throw new IllegalStateException("Error reading the version of the server", e);
        }
    }

    public static ServerInterface fromJar(Path pathToJar) throws Exception {

        InputHandler inputHandler = new InputHandler(System.in);
        System.setIn(inputHandler);

        if (Configuration.supressServerOutput) {
            System.setOut(new OutputHandler());
        }

        JarClassLoader loader = new JarClassLoader(pathToJar.toString());

        JarFile jarFile = loader.getJarFile();
        String versionId = getServerVersionId(jarFile);

        if (!VersionUtils.isSupported(versionId)) {
            loader.close();
            throw new IllegalArgumentException("Version not supported: " + versionId);
        }

        Mapping mapping;
        if (Configuration.mapping == null) {
            mapping = Mapping.getMappingFor(versionId);
        } else {
            mapping = Mapping.fromFile(Paths.get(Configuration.mapping));
        }

        ServerInterface result = new ServerInterface(loader, mapping, versionId, inputHandler);

        return result;
    }

    private final class ServerShutdownListener implements Runnable {

        @Override
        public void run() {
            do {
                try {
                    serverThread.join();
                } catch (InterruptedException e) {
                    //ignore
                }
            } while (serverThread.isAlive());
            setServerRunning(false);
        }
    }

    private static final class ServerStarter implements Runnable {
        private final String[] args;
        private final Method mainMethod;

        private ServerStarter(String[] args, Method mainMethod) {
            this.args = args;
            this.mainMethod = mainMethod;
        }

        @Override
        public void run() {
            try {
                mainMethod.invoke(null, new Object[] {args});
            } catch (Exception e) {
                throw new IllegalStateException("Error starting Minecraft server");
            }
        }
    }

}
