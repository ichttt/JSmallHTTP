package de.umweltcampus.webservices.file.compress;

import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.webservices.internal.brotli.BrotliCompressor;
import de.umweltcampus.webservices.internal.brotli.BrotliLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public class FileCompressor {
    private static final Logger LOGGER = LogManager.getLogger(FileCompressor.class);
    private static final PrecomputedHeader CONTENT_ENCODING_GZIP = PrecomputedHeader.create("Content-Encoding", "gzip");
    private static final PrecomputedHeader CONTENT_ENCODING_BROTLI = PrecomputedHeader.create("Content-Encoding", "br");
    private static final Map<Path, Object> LOCKS = new ConcurrentHashMap<>();
    private final CompressionStrategy compressionStrategy;
    private final Path toCompress;
    private final Path compressedFilesFolder;

    public FileCompressor(CompressionStrategy compressionStrategy, Path toCompress, Path compressedFilesFolder) {
        this.compressionStrategy = compressionStrategy;
        this.toCompress = toCompress;
        this.compressedFilesFolder = compressedFilesFolder;
    }

    public void precompress() {
        AtomicInteger processedFiles = new AtomicInteger(0);
        BrotliCompressor brotli = BrotliLoader.getBrotliCompressor();
        try (Stream<Path> stream = Files.walk(toCompress)) {
            stream.forEach(path -> {
                String fileName = path.getFileName().toString();
                if (compressionStrategy.shouldCompress(fileName) && Files.isRegularFile(path)) {
                    Path relativePath = toCompress.relativize(path);
                    Path inTmpGz = compressedFilesFolder.resolve(relativePath + ".gz");
                    Path inTmpBr = compressedFilesFolder.resolve(relativePath + ".br");
                    try {
                        FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                        compress(path, lastModifiedTime, inTmpGz);
                        if (brotli != null) {
                            compressBrotli(path, brotli, lastModifiedTime, inTmpBr);
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to compress file {}", path, e);
                    }
                    int i = processedFiles.incrementAndGet();
                    if (i % 1000 == 0) {
                        LOGGER.warn("{}: already compressed {} files!", toCompress, i);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to precompress files!", e);
        }
    }

    public Path getPathInCompressed(String allowedEncodings, String pathToFind, Path absolutePathToFind, FileTime srcLastModifiedTime, ResponseHeaderWriter headerWriter) {
        boolean shouldBrCompress;
        boolean shouldGzCompress;
        if (allowedEncodings != null && compressionStrategy.compress && compressionStrategy.shouldCompress(pathToFind)) {
            shouldBrCompress = allowedEncodings.contains("br");
            shouldGzCompress = allowedEncodings.contains("gzip");
            BrotliCompressor brotli = null;
            if (shouldBrCompress) {
                brotli = BrotliLoader.getBrotliCompressor();
                if (brotli == null) shouldBrCompress = false;
            }
            if (shouldGzCompress || shouldBrCompress) {
                Path inCompressed = compressedFilesFolder.resolve(pathToFind + (brotli != null ? ".br" : ".gz"));
                try {
                    if (!Files.exists(inCompressed) || (compressionStrategy.validateStored && !Files.getLastModifiedTime(inCompressed).equals(srcLastModifiedTime))) {
                        Object lock = LOCKS.computeIfAbsent(inCompressed, ignored -> new Object());
                        // synchronization on local var is desired here and safe - the object is controlled and only exists for this purpose
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (lock) {
                            // evaluate the condition again, maybe someone else already compressed while we waited for the lock
                            if (!Files.exists(inCompressed) || (compressionStrategy.validateStored && !Files.getLastModifiedTime(inCompressed).equals(srcLastModifiedTime))) {
                                if (brotli != null) {
                                    compressBrotli(absolutePathToFind, brotli, srcLastModifiedTime, inCompressed);
                                } else {
                                    compress(absolutePathToFind, srcLastModifiedTime, inCompressed);
                                }
                                LOCKS.remove(inCompressed);
                            }
                        }
                    }
                    if (brotli != null) {
                        headerWriter.addHeader(CONTENT_ENCODING_BROTLI);
                    } else {
                        headerWriter.addHeader(CONTENT_ENCODING_GZIP);
                    }
                    return inCompressed;
                } catch (IOException e) {
                    LOGGER.warn("Failed to compress file {}", absolutePathToFind, e);
                }
            }
        }
        return null;
    }


    private static void compress(Path src, FileTime srcLastModified, Path target) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target.getParent());
            target.toFile().deleteOnExit();
        }
        OutputStream outputStream = Files.newOutputStream(target);
        try (InputStream inputStream = Files.newInputStream(src);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            inputStream.transferTo(gzipOutputStream);
        }
        Files.setLastModifiedTime(target, srcLastModified);
    }

    private static void compressBrotli(Path src, BrotliCompressor compressor, FileTime srcLastModified, Path target) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target.getParent());
            target.toFile().deleteOnExit();
        }
        try (InputStream inputStream = Files.newInputStream(src);
             OutputStream brOutputStream = compressor.getCompressingOutputStream(target)) {
            inputStream.transferTo(brOutputStream);
        }
        Files.setLastModifiedTime(target, srcLastModified);
    }
}
