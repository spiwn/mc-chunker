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

import static org.iz.cs.chunker.io.ConsolePrinter.println;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.iz.cs.chunker.io.ConsolePrinter;
import org.iz.cs.chunker.minecraft.ServerInterface;

import com.google.gson.Gson;

public class Chunker {

    private static final String PROGRESS_FILE = "progress.json";
    public static List<String> DIMENSIONS = Arrays.asList("OVERWORLD", "NETHER", "END");
    private static final String CHUNKER_FOLDER = "chunker";
    public static ServerInterface server;

    public static PrintStream defaultOut;
    public static PrintStream defaultErr;

    public static void main(String[] args) throws Exception {
        Path currentDirectory = Paths.get(".").toAbsolutePath().normalize();

        ConsolePrinter.setOut(System.out);
        defaultOut = System.out;
        defaultErr = System.err;

        if (args.length == 0) {
            throw new IllegalArgumentException("Missing parameter. Must specify the server jar name");
        }

        Configuration.serverJar = args[0];

        boolean hasConfiguration = Configuration.loadConfiguration(currentDirectory);
        if (!hasConfiguration) {
            println("Empty " + Configuration.PROPERTIES + " generated. Please fill in and run again");
            return;
        }

        Path serverJarPath = currentDirectory.resolve(Configuration.serverJar);
        if (!Files.exists(serverJarPath)) {
            println("Server jar (" + Configuration.serverJar + ") not found");
            return;
        }
        serverJarPath = serverJarPath.toAbsolutePath().normalize();

        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandlerImplementation());

        server = ServerInterface.fromJar(serverJarPath);

        String[] serverArgs = new String[args.length - 1];
        System.arraycopy(args, 1, serverArgs, 0, serverArgs.length);
        server.startServer(serverArgs);

        waitForServerToLoad(server);

        GenerationProgress progress = null;
        Path progressPath = getChunkerPath().resolve(PROGRESS_FILE);
        if (Configuration.saveProgress && Files.exists(progressPath)) {
            progress = readProgressFile();
        }

        boolean deleteOldProgress = generateChunks(progress);

        if (Configuration.stop) {
            server.stopServer();
        }

        if (Configuration.saveProgress && deleteOldProgress) {
            Files.deleteIfExists(progressPath);
        }

        println("Chunker Done");
    }

    public static Path getChunkerPath() {
        return Paths.get(".", Chunker.CHUNKER_FOLDER);
    }

    private static void waitForServerToLoad(ServerInterface server) {
        println("Waiting for server to finish loading");
        int count = 0;
        int limit = 100;
        long pause = 1000L;
        while (!(Boolean) server.isServerReady() && count < limit) {
            try {
                Thread.sleep(pause);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            count++;
        }
        if (count >= limit) {
            throw new IllegalStateException("Server did not load in " + (limit) + " seconds");
        }
    }

    private static boolean generateChunks(GenerationProgress progress) throws Exception {
        GenerationProgress localProgress = progress;


        for (String dimension : Configuration.dimensions) {
            if (localProgress != null && !dimension.equals(localProgress.getDimension())) {
                continue;
            }

            boolean hasWorked = server.generateChunks(
                    dimension,
                    Configuration.x1, Configuration.x2,
                    Configuration.z1, Configuration.z2,
                    localProgress);
            localProgress = null;

            if (!server.isServerRunning()) {
                saveProgress(dimension, hasWorked);
                return false;
            }
        }
        println("Chunk generation done");
        return true;
    }

    private static boolean isDone(String dimension, int x, int z) {
        return Arrays.binarySearch(Configuration.dimensions, dimension) == Configuration.dimensions.length
                && x == Configuration.x2
                && z == Configuration.z2;
    }

    private static void saveProgress(String dimension, boolean hasWorked) {
        if (isDone(dimension, server.getLastX(), server.getLastZ())) {
            return;
        }
        GenerationProgress progress = new GenerationProgress();
        if (hasWorked) {
            progress.setX(server.getLastX());
            progress.setZ(server.getLastZ());
        }
        progress.setDimension(dimension);
        writeProgrssFile(progress);

    }

    private static GenerationProgress readProgressFile() {
        try (BufferedReader br = Files.newBufferedReader(
                getChunkerPath().resolve(PROGRESS_FILE),
                StandardCharsets.UTF_8)) {
            return LazyLoader.gson.fromJson(br, GenerationProgress.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not read progress file", e);
        }
    }

    private static void writeProgrssFile(GenerationProgress progress) {
        try (BufferedWriter bw = Files.newBufferedWriter(
                getChunkerPath().resolve(PROGRESS_FILE),
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE)) {
            LazyLoader.gson.toJson(progress, bw);
            bw.flush();
        } catch (IOException e) {
            defaultErr.println("Could not save progress");
            e.printStackTrace(defaultErr);
        }
    }

    private static final class UncaughtExceptionHandlerImplementation implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (server != null) {
                server.stopServer();
            }
            defaultErr.print("Exception in thread \""
                    + t.getName() + "\" ");
            e.printStackTrace(defaultErr);
            System.exit(1);
        }
    }

    private static class LazyLoader {
        static Gson gson = new Gson();
    }

}
