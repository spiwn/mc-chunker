package org.iz.cs.chunker.minecraft;

import static org.iz.cs.chunker.io.ConsolePrinter.println;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.iz.cs.chunker.Chunker;
import org.iz.cs.chunker.Configuration;
import org.iz.cs.chunker.JarClassLoader;
import org.iz.cs.chunker.Mapping;
import org.iz.cs.chunker.io.InputHandler;
import org.iz.cs.chunker.io.OutputHandler;
import org.iz.cs.chunker.minecraft.BehaviorManager.BehaviorName;
import org.iz.cs.chunker.minecraft.impl.GenerateChunk.GenerateChunkArguments;

import com.google.gson.Gson;

public class ServerInterface {

//  private static String CHUNK_ACCESS_CN = "net.minecraft.world.level.chunk.ChunkAccess";
//  private static String IS_UNSAVED_M = "isUnsaved";
//  private static String SET_UNSAVED_M = "setUnsaved";
//  private static String GET_STATUS_M = "getStatus";
//    private static final String LEVEL_CN = "net.minecraft.world.level.Level";
//    private static final String GET_CHUNK_M = "getChunk";
//    private static final String DEDICATED_SERVER_CN = "net.minecraft.server.dedicated.DedicatedServer";
//    private static final String MINECRAFT_SERVER_CN = "net.minecraft.server.MinecraftServer";
//    private static final String GET_LEVEL_M = "getLevel";
//    private static final String LEVEL_KEYS_M = "levelKeys";
//    private static final String IS_READY_F = "isReady";
//    private static final String RESOURCE_KEY_CN = "net.minecraft.resources.ResourceKey";
//  private static String LOCATION_M = "location";
//  private static String RESOURCE_LOCATION_CN = "net.minecraft.resources.ResourceLocation";
//  private static String GET_PATH_M = "getPath";

    private static final String SERVER_THREAD_NAME = "Server thread";

    public volatile boolean serverRunning = false;

    private InputHandler inputHandler;
    private JarClassLoader loader;
    private BehaviorManager bm;
    private ThreadGroup minecraftThreadGroup;

    private ServerInterface(JarClassLoader loader, Mapping mapping, String versionId, InputHandler inputHandler) throws Exception {
        this.loader = loader;
        this.inputHandler = inputHandler;
        this.bm = new BehaviorManager(versionId, new ClassCache(mapping, loader), mapping, this);

        String dedicatedServerClassName = (String) bm.get(BehaviorName.GET_DEDICATED_SERVER_CLASS_NAME).apply(null);

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
        Thread t = new Thread(minecraftThreadGroup, new RunnableImplementation(args, mainMethod), "Server starter");
        t.setContextClassLoader(loader);
        t.start();
        long seconds = 100L;
        t.join(seconds * 1000L);
        if (t.isAlive()) {
            serverRunning = false;
            throw new IllegalStateException("Server did not start in " + seconds + " seconds");
        }
        serverRunning = true;

        println("Minecreaft server started");
    }

    public Boolean isServerReady() {
        return (Boolean) bm.get(BehaviorName.IS_SERVER_READY).apply(null);
    }

    public void generateChunk(String dimension, int x1, int x2, int z1, int z2) {
        int total = (x2 - x1 + 1) * (z2 - z1 + 1);
        int counter = 0;

        int step;
        float percentIncrement;

        println("Starting generating chunks for area (" + x1 + ", " + z1 + ") (" + x2 + ", " + z2 + "), "
                + "dimension " + dimension + ". "
                + "Area contains " + total + " total chunks" );

        if (total < 200) {
            step = 1;
            percentIncrement = 100f / (float) total;
        } else {
            step = total / 200;
            percentIncrement = 0.5f;
        }
        float progress = 0;

        GenerateChunkArguments args = new GenerateChunkArguments(dimension, 0, 0);

        long start = System.currentTimeMillis();
        for (int i = x1; i <= x2; i++) {
            for (int j = z1; j <= z2; j++) {
                args.setX(x1);
                args.setZ(z1);
                bm.get(BehaviorName.GENERATE_CHUNK).apply(args);
                if (++counter == step) {
                    counter = 0;
                    progress += percentIncrement;
                    long time = System.currentTimeMillis() - start;
                    println("Progress: " + progress + "% Elapsed: " + (float) time/ 1000 + "s Remaining estimate: " +  ((time * (100 / progress) - time) / 1000) + "s");
                }
            }
        }
        println("Done generating chunk in dimension " + dimension);
    }

    public void stopServer() {
        if (serverRunning) {
            println("Stopping server");
            inputHandler.enqueue("stop\n", StandardCharsets.UTF_8, true);


            Thread[] threads = new Thread[64];
            minecraftThreadGroup.enumerate(threads, false);
            for (Thread thread : threads) {
                if (SERVER_THREAD_NAME.equals(thread.getName())) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        Chunker.defaultErr.print("Interupted while waiting for server to stop.");
                        e.printStackTrace(Chunker.defaultErr);
                    }
                    break;
                }
            }
        } else {
            println("Server seems to not be running. Skipping attempt to stop it");
        }
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

    public static ServerInterface fromJar(Path pathToJar, String[] args) throws Exception {

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

        Mapping mapping = Mapping.getMappingFor(versionId);

        ServerInterface result = new ServerInterface(loader, mapping, versionId, inputHandler);

        return result;
    }

    private final class RunnableImplementation implements Runnable {
        private final String[] args;
        private final Method mainMethod;

        private RunnableImplementation(String[] args, Method mainMethod) {
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
