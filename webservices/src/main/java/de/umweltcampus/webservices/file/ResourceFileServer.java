package de.umweltcampus.webservices.file;

import de.umweltcampus.smallhttp.base.HTTPRequest;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.webservices.file.compress.CompressionStrategy;
import de.umweltcampus.webservices.internal.loader.Loader;
import de.umweltcampus.webservices.internal.util.JPMSUtil;
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
        return new FileServerModule(pathToServe, prefixToServe, CompressionStrategy.compressAndStore(Loader.DEV_MODE, true), webservice, additionalHeaderAdder);
    }

    private static Path copyFiles(WebserviceBase webservice, String pathInService) throws IOException {
        if (pathInService.startsWith("/")) {
            pathInService = pathInService.substring(1);
        }

        Class<? extends WebserviceBase> webserviceClass = webservice.getClass();
        JPMSUtil.SourceType sourceTypeForClass = JPMSUtil.getSourceTypeForClass(webserviceClass);


        switch (sourceTypeForClass) {
            case JAR -> {
                URL location = JPMSUtil.getLocation(webserviceClass);
                return copyJarSource(webservice, pathInService, location).toRealPath();
            }
            case EXPLODED_DIR -> {
                // We actually don't even need to copy the dir, we are already working on an exploded directory
                return JPMSUtil.getPathInExplodedDirectorySource(pathInService, webserviceClass).toRealPath();
            }
            default -> throw new RuntimeException("Unknown type " + sourceTypeForClass);
        }
    }

    private static Path copyJarSource(WebserviceBase webservice, String pathInService, URL location) throws IOException {
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

}
