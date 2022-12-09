package de.umweltcampus.webservices.internal.util;

import de.umweltcampus.webservices.file.ResourceFileServer;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class JPMSUtil {
    public enum SourceType {
        JAR, EXPLODED_DIR
    }

    public static URL getLocation(Class<?> clazz) {
        return Objects.requireNonNull(clazz.getProtectionDomain().getCodeSource().getLocation());
    }

    public static SourceType getSourceTypeForClass(Class<?> clazz) {
        URL location = getLocation(clazz);
        URI uri;
        try {
            uri = location.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("What? A URL could not be converted to a URI? Class: " + clazz + ", URL: " + location, e);
        }
        Path path = Paths.get(uri);
        if (location.toString().endsWith(".jar") && Files.isRegularFile(path)) {
            return SourceType.JAR;
        } else if (location.getProtocol().equals("file") && Files.isDirectory(path)) {
            return SourceType.EXPLODED_DIR;
        } else {
            throw new RuntimeException("Unknown source type for " + clazz + " of url " + uri);
        }
    }

    public static Path getPathInExplodedDirectorySource(String pathToFind, Class<?> clazz) {
        if (pathToFind.startsWith("/")) {
            pathToFind = pathToFind.substring(1);
        }

        URL location = getLocation(clazz);
        URI locationURI;
        try {
            locationURI = location.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to convert URL to URI. How does this even work?!", e);
        }
        Path rootPath = Paths.get(locationURI);
        Path resolved = rootPath.resolve(pathToFind);
        if (!Files.exists(resolved)) {
            // Don't fail yet - some IDEs put the resources in a different directory. Try looking that up
            String fileName = rootPath.getFileName().toString();
            // Go up THREE dirs and try to find a resources directory for the same source set
            Path possibleDifferentRoot = rootPath.resolve("../../../resources/" + fileName);
            if (Files.isDirectory(possibleDifferentRoot)) {
                resolved = possibleDifferentRoot.resolve(pathToFind);
                if (!Files.exists(resolved)) {
                    // Nope, still not valid
                    throw new RuntimeException("Failed to find file " + fileName + " in " + clazz + " !");
                }
                // If we reach here this different root is the one we are looking for. Use the newly resolved path
            }
        }
        return resolved;
    }
}
