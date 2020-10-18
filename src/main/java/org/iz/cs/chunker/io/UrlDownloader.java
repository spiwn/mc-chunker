package org.iz.cs.chunker.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


public class UrlDownloader {

    /**
     * Very simple download code
     */
    public static void downloadToFile(String urlString, Path filePath)
            throws URISyntaxException, InterruptedException, MalformedURLException {
        URL url = new URI(urlString).toURL();
        try (InputStream is = url.openStream()) {
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Error downloading file from " + url, e);
        }

    }
}
