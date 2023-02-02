package de.nocoffeetech.webservices.core.file;

import de.nocoffeetech.webservices.core.file.compress.CompressionStrategy;
import de.nocoffeetech.webservices.core.internal.util.JPMSUtil;
import de.nocoffeetech.webservices.core.internal.util.TempDirHelper;
import de.nocoffeetech.webservices.core.service.WebserviceBase;
import de.nocoffeetech.smallhttp.base.HTTPRequest;
import de.nocoffeetech.smallhttp.header.PrecomputedHeader;
import de.nocoffeetech.smallhttp.response.ResponseHeaderWriter;
import de.nocoffeetech.webservices.core.internal.loader.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
            pathToServe = copyFilesToTemp(webservice, pathInService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy files for module " + webservice.getInstanceName(), e);
        }
        return new FileServerModule(pathToServe, prefixToServe, CompressionStrategy.compressAndStore(Loader.DEV_MODE, true), webservice, additionalHeaderAdder);
    }

    public static Path copyFilesToTemp(WebserviceBase webservice, String pathInService) throws IOException {
        if (pathInService.startsWith("/")) {
            pathInService = pathInService.substring(1);
        }

        Class<? extends WebserviceBase> webserviceClass = webservice.getClass();
        JPMSUtil.SourceType sourceTypeForClass = JPMSUtil.getSourceTypeForClass(webserviceClass);


        switch (sourceTypeForClass) {
            case JAR -> {
                return copyJarSource(webservice, pathInService).toRealPath();
            }
            case EXPLODED_DIR -> {
                // We actually don't even need to copy the dir, we are already working on an exploded directory
                return JPMSUtil.getPathInExplodedDirectorySource(pathInService, webserviceClass).toRealPath();
            }
            default -> throw new RuntimeException("Unknown type " + sourceTypeForClass);
        }
    }

    private static Path copyJarSource(WebserviceBase webservice, String pathInService) throws IOException {
        try (FileSystem fileSystem = JPMSUtil.openFileSystemFor(webservice.getClass())) {
            Path rootInFs = fileSystem.getPath(pathInService);
            List<Path> paths;
            try (Stream<Path> pathStream = Files.walk(rootInFs)) {
                paths = pathStream.toList();
            }
            Path targetDir = TempDirHelper.createTempPathFor(webservice.getInstanceName(), pathInService);
            for (Path source : paths) {
                if (Files.isDirectory(source)) continue;
                String relativePath = rootInFs.relativize(source).toString();
                Path pathInTarget = targetDir.resolve(relativePath);
                Files.createDirectories(pathInTarget.getParent());
                Files.copy(source, pathInTarget);
            }

            LOGGER.info("Processed {} files for service {}", paths.size(), webservice.getInstanceName());
            return targetDir;
        }
    }

}
