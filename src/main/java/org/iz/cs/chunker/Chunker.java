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
package org.iz.cs.chunker;

import static org.iz.cs.chunker.ConsolePrinter.println;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.Gson;

public class Chunker {

    public static List<String> DIMENSIONS = Arrays.asList("OVERWORLD", "NETHER", "END");

//    private static String CHUNK_ACCESS_CN = "net.minecraft.world.level.chunk.ChunkAccess";
//    private static String IS_UNSAVED_M = "isUnsaved";
//    private static String SET_UNSAVED_M = "setUnsaved";
//    private static String GET_STATUS_M = "getStatus";
    private static final String LEVEL_CN = "net.minecraft.world.level.Level";
    private static final String GET_CHUNK_M = "getChunk";
    private static final String DEDICATED_SERVER_CN = "net.minecraft.server.dedicated.DedicatedServer";
    private static final String MINECRAFT_SERVER_CN = "net.minecraft.server.MinecraftServer";
    private static final String GET_LEVEL_M = "getLevel";
    private static final String LEVEL_KEYS_M = "levelKeys";
    private static final String IS_READY_F = "isReady";
    private static final String RESOURCE_KEY_CN = "net.minecraft.resources.ResourceKey";
//    private static String LOCATION_M = "location";
//    private static String RESOURCE_LOCATION_CN = "net.minecraft.resources.ResourceLocation";
//    private static String GET_PATH_M = "getPath";

    public static void checkIdentifiers(Mapping mapping) {
        mapping.getClassName(DEDICATED_SERVER_CN).length();
        mapping.getClassName(MINECRAFT_SERVER_CN).length();
        mapping.getMethod(MINECRAFT_SERVER_CN, GET_LEVEL_M, RESOURCE_KEY_CN).length();
        mapping.getMethod(MINECRAFT_SERVER_CN, LEVEL_KEYS_M).length();
        mapping.getClassName(RESOURCE_KEY_CN).length();
    }

    public static void main(String[] args) throws Exception {
        Path currentDirectory = Paths.get(".").toAbsolutePath().normalize();

        ConsolePrinter.setOut(System.out);

//        Configuration.createEmptyPropertiesFile(currentDirectory);

        if (args.length == 0) {
            throw new IllegalArgumentException("Missing parameter. Must specify the server jar name");
        }

        Configuration.serverJar = args[0];

        boolean hasConfiguration = Configuration.loadConfiguration(currentDirectory);
        if (!hasConfiguration) {
            println("Empty " + Configuration.PROPERTIES + " generated. Please fill in and run again");
            return;
        }

        InputHandler inputHandler = null;
        if (Configuration.stop) {
            inputHandler = new InputHandler(System.in);
            System.setIn(inputHandler);
        }

        if (Configuration.supressServerOutput) {
            System.setOut(new OutputHandler());
        }


        Path serverJar = currentDirectory.resolve(Configuration.serverJar);
        if (!Files.exists(serverJar)) {
            println("Server jar (" + Configuration.serverJar + ") not found");
            return;
        }
        serverJar = serverJar.toAbsolutePath().normalize();

        JarClassLoader loader = new JarClassLoader(serverJar.toString());

        JarFile jarFile = loader.getJarFile();
        String versionId = getServerVersionId(jarFile);

        Mapping mapping = Mapping.getMappingFor(versionId);
        checkIdentifiers(mapping);

        String dedicatedServerClassName = mapping.getClassName(DEDICATED_SERVER_CN);

        loader.setClassToDecorate(dedicatedServerClassName);

        Class<?> dedicatedServerClass = loader.loadClass(dedicatedServerClassName);

        String[] serverArgs = new String[args.length - 1];
        System.arraycopy(args, 1, serverArgs, 0, serverArgs.length);
        startMinecraftServer(loader, serverArgs);

        Field instanceField = dedicatedServerClass.getDeclaredField("instance");
        Object dedicatedServer = instanceField.get(null);

        generateChunks(mapping, loader, dedicatedServer);

        if (inputHandler != null) {
            inputHandler.enqueue("stop\n", StandardCharsets.UTF_8);
        }

        println("Chunker Done");
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
            Map<String, Object> m = (Map<String, Object>) g.fromJson(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)), Map.class);

            return (String) m.get("id");
        } catch (IOException e) {
            throw new IllegalStateException("Error reading the version of the server", e);
        }
    }

    private static void generateChunks(Mapping mapping, JarClassLoader loader, Object dedicatedServer)
            throws Exception {

        Class<?> mc_s_cl = loader.loadClass(mapping.getClassName(MINECRAFT_SERVER_CN));
        Class<?> rk_cl = loader.loadClass(mapping.getClassName(RESOURCE_KEY_CN));
        Method getLevel_m = mc_s_cl.getMethod(mapping.getMethod(MINECRAFT_SERVER_CN, GET_LEVEL_M, RESOURCE_KEY_CN), rk_cl);
        Field isReady_f = mc_s_cl.getDeclaredField(mapping.getField(MINECRAFT_SERVER_CN, IS_READY_F));
        isReady_f.setAccessible(true);
        Class<?> level_cl = loader.loadClass(mapping.getClassName(LEVEL_CN));
        Method getChunk_m = level_cl.getDeclaredMethod(
                mapping.getMethod(LEVEL_CN, GET_CHUNK_M, "int", "int"), int.class, int.class);


        println("Waiting for server to load");

        for (String dimension : Configuration.dimensions) {

            Field level_dimension_f = level_cl.getDeclaredField(mapping.getField(LEVEL_CN, dimension));
            Object overworldLevelKey = level_dimension_f.get(null);

            int count = 0;
            int limit = 100;
            long pause = 1000L;
            while (!(Boolean) isReady_f.get(dedicatedServer) && count < limit) {
                Thread.sleep(pause);
                count++;
            }
            Object level = getLevel_m.invoke(dedicatedServer, overworldLevelKey);

            if (level == null) {
                throw new IllegalArgumentException("Server did not load dimension " + dimension + "in " + (limit * pause) / 1000L + "s");
            }

//        Class<?> chunk_access_cl = loader.loadClass(mapping.getClassName(CHUNK_ACCESS_CN));
//        Method isUnsaved_m = chunk_access_cl.getDeclaredMethod(mapping.getMethod(CHUNK_ACCESS_CN, IS_UNSAVED_M));
//        Method getStatus_m = chunk_access_cl.getDeclaredMethod(mapping.getMethod(CHUNK_ACCESS_CN, "getStatus"));
//        Method setUnsaved_m = chunk_access_cl.getDeclaredMethod(mapping.getMethod(CHUNK_ACCESS_CN, SET_UNSAVED_M, "boolean"), boolean.class);

            int x1 = Configuration.x1;
            int x2 = Configuration.x2;
            int z1 = Configuration.z1;
            int z2 = Configuration.z2;

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

            long start = System.currentTimeMillis();
            for (int i = x1; i <= x2; i++) {
                for (int j = z1; j <= z2; j++) {
                    getChunk_m.invoke(level, i, j);
                    if (counter++ == step) {
                        counter = 0;
                        progress += percentIncrement;
                        long time = System.currentTimeMillis() - start;
                        println("Progress: " + progress + "% Elapsed: " + (float) time/ 1000 + "s Remaining estimate: " +  ((time * (100 / progress) - time) / 1000) + "s");
                    }
                }
            }
            println("Done generating chunk in dimension " + dimension);
        }
        println("Chunk generation done");
    }

    private static void startMinecraftServer(JarClassLoader loader, String[] args) throws Exception {
        println("Starting Minecraft server");
        String main = loader.getMain();

        Class<?> mainClazz = loader.loadClass(main);

        Method mainMethod = mainClazz.getDeclaredMethod("main", String[].class);

        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    mainMethod.invoke(null, new Object[] {args});
                } catch (Exception e) {
                    //TODO:
                    throw new IllegalStateException("Error starting Minecraft server");
                }
            }
        });
        t.setContextClassLoader(loader);
        t.start();
        t.join();

        println("Minecreaft server started");
    }

}
