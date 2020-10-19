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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
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

import org.iz.cs.chunker.io.ConsolePrinter;
import org.iz.cs.chunker.io.InputHandler;
import org.iz.cs.chunker.io.OutputHandler;
import org.iz.cs.chunker.minecraft.ServerInterface;

import com.google.gson.Gson;

public class Chunker {

    public static List<String> DIMENSIONS = Arrays.asList("OVERWORLD", "NETHER", "END");
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

        generateChunks(server);

        if (Configuration.stop) {
            server.stopServer();
        }

        println("Chunker Done");
    }

    private static void waitForServerToLoad(ServerInterface server) {
        println("Waiting for server to load");
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

    private static void generateChunks(ServerInterface server) throws Exception {

        for (String dimension : Configuration.dimensions) {
            server.generateChunks(dimension, Configuration.x1, Configuration.x2, Configuration.z1, Configuration.z2);
        }
        println("Chunk generation done");
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

}
