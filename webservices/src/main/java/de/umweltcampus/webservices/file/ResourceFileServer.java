package de.umweltcampus.webservices.file;

import de.umweltcampus.smallhttp.base.HTTPRequest;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.webservices.file.compress.CompressionStrategy;
import de.umweltcampus.webservices.internal.util.TempDirHelper;
import de.umweltcampus.webservices.service.WebserviceBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * A module that serves contents from the resources of a jar by copying them
 * to a temp directory and serving them from there using the default {@link FileServerModule}
 */
public class ResourceFileServer {
    private static final Logger LOGGER = LogManager.getLogger(ResourceFileServer.class);
    private static final PrecomputedHeader CACHE_CONTROL = PrecomputedHeader.create("Cache-Control", "max-age: 3600");

    public static FileServerModule createModule(WebserviceBase webservice, String prefixToServe, String pathInService) {
        return createModule(webservice, prefixToServe, pathInService, (httpRequest, responseHeaderWriter) -> responseHeaderWriter.addHeader(CACHE_CONTROL));
    }

    public static FileServerModule createModule(WebserviceBase webservice, String prefixToServe, String pathInService, BiConsumer<HTTPRequest, ResponseHeaderWriter> additionalHeaderAdder) {
        Path pathToServe;
        try {
            pathToServe = copyFiles(webservice, pathInService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy files for module " + webservice.getName(), e);
        }
        return new FileServerModule(pathToServe, prefixToServe, CompressionStrategy.compressAndStore(false, true), webservice, additionalHeaderAdder);
    }

    private static Path copyFiles(WebserviceBase webservice, String pathInService) throws IOException {
        if (pathInService.startsWith("/")) pathInService = pathInService.substring(1);
        URL location = webservice.getClass().getProtectionDomain().getCodeSource().getLocation();

        if (location.toString().endsWith(".jar")) {
            return copyJarSource(webservice, pathInService, location).toRealPath();
        } else if (location.getProtocol().equals("file")) {
            return copyExplodedDirSource(pathInService, location).toRealPath();
        } else {
            throw new RuntimeException("Failed to handle code source location protocol type " + location.getProtocol() + " (complete url:" + location + ")");
        }
    }

    private static Path copyJarSource(WebserviceBase webservice, String pathInService, URL location) throws IOException {
        // TODO validate the jar code actually works
        String file = location.getFile();
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
            file = file.substring(1); // Strip initial slash, windows has no root, but different letters for its drives. URL doesn't know this and thus returns an invalid windows path with a starting slash
        }
        try (FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(file), Map.of(), webservice.getClass().getClassLoader())) {
            Path rootInFs = fileSystem.getPath(pathInService);
            List<Path> paths;
            try (Stream<Path> pathStream = Files.walk(rootInFs)) {
                paths = pathStream.toList();
            }
            Path targetDir = TempDirHelper.createTempPathFor(webservice.getName(), pathInService);
            for (Path source : paths) {
                if (Files.isDirectory(source)) continue;
                String relativePath = rootInFs.relativize(source).toString();
                Path pathInTarget = targetDir.resolve(relativePath);
                Files.copy(source, pathInTarget);
            }

            LOGGER.info("Processed {} files for service {}", paths.size(), webservice.getName());
            return targetDir;
        }
    }

    private static Path copyExplodedDirSource(String pathInService, URL location) {
        URI locationURI;
        try {
            locationURI = location.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to convert URL to URI. How does this even work?!", e);
        }
        Path rootPath = Paths.get(locationURI);
        Path resolved = rootPath.resolve(pathInService);
        if (!Files.exists(resolved)) {
            // Don't fail yet - some IDEs put the resources in a different directory. Try looking that up
            String fileName = rootPath.getFileName().toString();
            // Go up THREE dirs and try to find a resources directory for the same source set
            Path possibleDifferentRoot = rootPath.resolve("../../../resources/" + fileName);
            if (Files.isDirectory(possibleDifferentRoot)) {
                resolved = possibleDifferentRoot.resolve(pathInService);
                if (!Files.exists(resolved)) {
                    // Nope, still not valid
                    throw new RuntimeException("Failed to find directory root");
                }
                // If we reach here this different root is the one we are looking for. Use the newly resolved path
            }
        }
        // We actually don't even need to copy the dir, we are already working on an exploded directory
        // So just return the path to the exploded dir
        return resolved;
    }

}
