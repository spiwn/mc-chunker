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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Configuration {


    public  static final String PROPERTIES = "chunker.properties";

    public static String serverJar = null;
    public static Integer x1 = null;
    public static Integer z1 = null;
    public static Integer x2 = null;
    public static Integer z2 = null;
    public static String mapping = null;
    public static String[] dimensions = null;
    public static Boolean stop = null;
    public static Boolean defaultBehaviors = null;

    public static Boolean supressServerOutput = null;
    private static final String SUPPRESS_SERVER_OUTPUT = "suppress-server-output";


    public static Properties getDefaults() {
        Properties defaults = new Properties();
        defaults.setProperty("dimension", "OVERWORLD");
        defaults.setProperty("stop", "false");
        defaults.setProperty(SUPPRESS_SERVER_OUTPUT, "false");
        defaults.setProperty("default-behaviors", "false");
        return defaults;
    }

    public static void createEmptyPropertiesFile(Path directory) {
        Properties props = new Properties();
        props.putAll(getDefaults());
        props.setProperty("x1", "0");
        props.setProperty("z1", "0");
        props.setProperty("x2", "0");
        props.setProperty("z2", "0");
        props.setProperty("mapping", "pathToFile");
        try (BufferedWriter writer = Files.newBufferedWriter(directory.resolve(PROPERTIES))) {
            props.store(writer, "Configuration for Chunker");
        } catch (IOException e) {
            throw new IllegalStateException("Error creating an empty properties file", e);
        }
    }

    public static boolean loadConfiguration(Path directory) {
        Path filePath = directory.resolve(PROPERTIES);

        if (!Files.exists(filePath)) {
            createExampleProperties(filePath);
            return false;
        }

        Properties props = new Properties(getDefaults());
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            props.load(br);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading " + PROPERTIES);
        }

        parseProperties(props);

        return true;
    }

    private static void parseProperties(Properties props) {
        x1 = getInteger(props, "x1");
        z1 = getInteger(props, "z1");
        x2 = getInteger(props, "x2");
        z2 = getInteger(props, "z2");

        Integer small;
        Integer big;

        small = Math.min(x1, x2);
        big = Math.max(x1, x2);
        x1 = small;
        x2 = big;

        small = Math.min(z1, z2);
        big = Math.max(z1, z2);
        z1 = small;
        z2 = big;

        mapping = props.getProperty("mapping");

        dimensions = props.getProperty("dimension").toUpperCase().split(",");
        for (int i = 0; i < dimensions.length; i++) {
            dimensions[i] = dimensions[i].trim();
            if (!Chunker.DIMENSIONS.contains(dimensions[i])) {
                throw new IllegalArgumentException("Invalid value for dimension property");
            }
        }

        stop = Boolean.valueOf(props.getProperty("stop"));
        supressServerOutput = Boolean.valueOf(props.getProperty(SUPPRESS_SERVER_OUTPUT));

        defaultBehaviors = Boolean.valueOf(props.getProperty("default-behaviors"));
    }

    private static int getInteger(Properties props, String key) {
        String string = (String) props.get(key);
        if (string == null) {
            throw new IllegalArgumentException("Missing property: " + key);
        }
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for " + key);
        }
    }

    private static void createExampleProperties(Path filePath) {
        InputStream is = Configuration.class.getResourceAsStream(PROPERTIES);
        try {
            Files.copy(is, filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Error creating " + PROPERTIES, e);
        }
    }

}
