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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iz.cs.chunker.minecraft.Constants;
import org.iz.cs.chunker.minecraft.VersionUtils;

import com.google.gson.Gson;

public class Mapping {

    private static final String VERSION_MANIFEST_JSON = "version_manifest.json";
    private static final String VERSIONS_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String CHUNKER_FOLDER = "chunker";

    private static final String NAME = ".name";
    private static final String METHOD = "m";
    private static final String FIELD = "f";

    private static volatile Map<String, Object> cachedManifest;

    private Map<String, Map<String, String>> map;

    private Mapping(Map<String, Map<String, String>> map) {
        this.map = map;
    }

    public String getClassName(String originalClass) {
        return map.get(originalClass).get(NAME);
    }

    public String getField(String originalClass, String originalField) {
        return map.get(originalClass).get(FIELD + " " + originalField);
    }

    public String getMethod(String originalClass, String originalMethod) {
        return map.get(originalClass).get(METHOD + " " + originalMethod);
    }

    public String getMethod(String originalClass, String originalMethod, String...parameters) {
        StringBuilder builder = new StringBuilder();
        builder.append(METHOD);
        builder.append(" ");
        builder.append(originalMethod);
        if (parameters.length > 0) {
            builder.append(" ");
            builder.append(parameters[0]);
            for (int i = 1; i < parameters.length; i++) {
                builder.append(",");
                builder.append(parameters[i]);
            }
        }
        return map.get(originalClass).get(builder.toString());
    }

    public static Mapping getMappingFor(String versionId) {
        try {
            Mapping result = getMappingForInternal(versionId);
            result.checkIdentifiers();
            return result;
        } catch (MalformedURLException | URISyntaxException e) {
            // This should not happen
            throw new IllegalStateException("A URL was invalid");
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interupted while loading the mapping", e);
        }
    }

    private void checkIdentifiers() {
        getClassName(Constants.DEDICATED_SERVER_CN).length();
        getClassName(Constants.MINECRAFT_SERVER_CN).length();
        getMethod(Constants.MINECRAFT_SERVER_CN, Constants.GET_LEVEL_M, Constants.RESOURCE_KEY_CN).length();
        getMethod(Constants.MINECRAFT_SERVER_CN, Constants.LEVEL_KEYS_M).length();
        getClassName(Constants.RESOURCE_KEY_CN).length();
    }

    private static Mapping getMappingForInternal(String versionId)
            throws URISyntaxException, InterruptedException, MalformedURLException {
        Path chunkerPath = getChunkerPath();

        try {
            Files.createDirectories(chunkerPath);
        } catch (IOException e1) {
            throw new IllegalStateException("Could not create chunker directory");
        }

        Path clientJsonPath = chunkerPath.resolve("client_" + versionId + ".json");
        boolean hasClientJson = Files.exists(clientJsonPath);
        if (!hasClientJson) {
            String clientJsonUrl = getClientJsonUrl(versionId, false);
            println("Downloading client.json for " + versionId);
            downloadToFile(clientJsonUrl, clientJsonPath);
        }

        Path mappingPath = chunkerPath.resolve("server_mapping_" + versionId + ".txt");
        boolean hasMappingForVersion = Files.exists(mappingPath);
        if (!hasMappingForVersion) {
            String mappingUrl = getMappingUrl(clientJsonPath);
            println("Downloading server mapping for " + versionId);
            downloadToFile(mappingUrl, mappingPath);
        }

        try {
            return loadMapping(mappingPath);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading mapping file", e);
        }
    }

    private static Path getChunkerPath() {
        return Paths.get(".", CHUNKER_FOLDER);
    }

    private static Path getManifestJsonPath() {
        return getChunkerPath().resolve(VERSION_MANIFEST_JSON);
    }

    @SuppressWarnings({ "unchecked" })
    private static String getMappingUrl(Path clientJsonPath) {
        try (BufferedReader reader = Files.newBufferedReader(clientJsonPath)) {
            Map<String, Object> clientJson = LazyLoader.gson.fromJson(reader, Map.class);
            Map<String, Object> downloads = (Map<String, Object>) clientJson.get("downloads");
            Map<String, Object> server = (Map<String, Object>) downloads.get("server_mappings");
            return (String) server.get("url");
        } catch (IOException e) {
            throw new IllegalStateException("Could not read client.json file", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String getClientJsonUrl(String versionId, boolean forced)
            throws URISyntaxException, InterruptedException {
        Map<String, Object> manifest = readManifestJson();
        List<Object> versionsList = (List<Object>) manifest.get("versions");
        Map<String, Object> version = null;
        for (Object object : versionsList) {
            Map<String, Object> current = (Map<String, Object>) object;
            if (current.get("id").equals(versionId)) {
                version = current;
                break;
            }
        }
        if (version == null) {
            if (forced) {
                // I am too lazy to handle this in a better way
                throw new IllegalArgumentException("Version not found. It may not be supported: " + versionId);
            }
            try {
                downloadToFile(VERSIONS_MANIFEST_URL, getManifestJsonPath());
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + VERSION_MANIFEST_JSON + " file", e);
            }
            return getClientJsonUrl(versionId, true);
        }

        return (String) version.get("url");

    }

    private static void downloadManifest() {
        println("Downloading versions manifest");
        cachedManifest = null;
        try {
            downloadToFile(VERSIONS_MANIFEST_URL, getManifestJsonPath());
        } catch (MalformedURLException | URISyntaxException | InterruptedException e) {
            throw new IllegalStateException("Could not download manifest", e);
        }

    }

    public static Map<String, Object> readManifestJson() {
        if (cachedManifest != null) {
            return cachedManifest;
        }

        Path manifestPath = getManifestJsonPath();
        if (!Files.exists(manifestPath)) {
            downloadManifest();
        }

        try (BufferedReader reader = Files.newBufferedReader(getManifestJsonPath())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = LazyLoader.gson.fromJson(reader, Map.class);
            if (result != null) {
                cachedManifest = result;
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + VERSION_MANIFEST_JSON + " file", e);
        }
    }

    private static void downloadToFile(String urlString, Path filePath)
            throws URISyntaxException, InterruptedException, MalformedURLException {
        URL url = new URI(urlString).toURL();
        try (InputStream is = url.openStream()) {
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Error downloading file from " + url, e);
        }

    }

    private static Mapping loadMapping(Path mappingPath) throws IOException {
        Pattern fieldPattern = Pattern.compile(
                "\\s+"
                + "(?<type>[a-zA-Z0-9_$.]+(?:\\[\\])*)"
                + " "
                + "(?<name>[$a-zA-Z0-9_]+)"
                + " -> "
                + "(?<obfuscatedName>[a-zA-Z0-9_]+)");
        Pattern methodPattern = Pattern.compile(
                "\\s+"
                + "(?:\\d+:\\d+:)?"
                + "(?<retunType>[a-zA-Z0-9_$.]+(?:\\[\\])*)"
                + " "
                + "(?<name>[a-zA-Z0-9_$<>]+)"
                + "\\("
                + "(?<parameters>(?:[a-zA-Z0-9_$.]+(?:\\[\\])*)?(?:,[a-zA-Z0-9_$.]+(?:\\[\\])*)*)"
                + "\\)"
                + " -> "
                + "(?<obfuscatedName>[a-zA-Z0-9_$<>]+)");

        Map<String, Map<String, String>> mapping = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(mappingPath)) {
            HashMap<String, String> currentClassMap = null;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith(" ")) {
                    Matcher matcher = fieldPattern.matcher(line);
                    boolean isField = matcher.matches();
                    if (isField) {
                        currentClassMap.put(FIELD + " " + matcher.group("name"), matcher.group("obfuscatedName"));
                        continue;
                    }
                    matcher = methodPattern.matcher(line);
                    boolean isMethod = matcher.matches();
                    if (isMethod) {
                        String parameters = matcher.group("parameters");
                        String key = METHOD + " " + matcher.group("name");
                        if (!parameters.isEmpty()) {
                            key = key + " " + parameters;
                        }
                        currentClassMap.put(key, matcher.group("obfuscatedName"));
                        continue;
                    }
                } else {
                    String[] split = line.split(" -> ");
                    currentClassMap = new HashMap<String, String>();
                    currentClassMap.put(NAME, split[1].substring(0, split[1].length() - 1));
                    mapping.put(split[0], currentClassMap);
                }
            }
        }
        return new Mapping(mapping);
    }

    private static class LazyLoader {
        static Gson gson = new Gson();
    }

}
