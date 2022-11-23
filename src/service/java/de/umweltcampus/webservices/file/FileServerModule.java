package de.umweltcampus.webservices.file;

import de.umweltcampus.smallhttp.base.HTTPRequest;
import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.BuiltinHeaders;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.response.FixedResponseBodyWriter;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;
import de.umweltcampus.smallhttp.util.ResponseDateFormatter;
import de.umweltcampus.webservices.internal.brotli.BrotliCompressor;
import de.umweltcampus.webservices.internal.brotli.BrotliLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

public class FileServerModule {
    private static final Logger LOGGER = LogManager.getLogger(FileServerModule.class);
    private static final PrecomputedHeader ALLOW_HEADER = new PrecomputedHeader(new PrecomputedHeaderKey("Allow"), Stream.of(Method.OPTIONS, Method.GET, Method.HEAD).map(Enum::name).collect(Collectors.joining(", ")));
    private static final PrecomputedHeader CONTENT_ENCODING_GZIP = new PrecomputedHeader(new PrecomputedHeaderKey("Content-Encoding"), "gzip");
    private static final PrecomputedHeader CONTENT_ENCODING_BROTLI = new PrecomputedHeader(new PrecomputedHeaderKey("Content-Encoding"), "br");
    private static final PrecomputedHeaderKey LAST_MODIFIED = new PrecomputedHeaderKey("Last-Modified");
    private static final ZoneId GMT = ZoneId.of("GMT");
    private final Path baseDirToServe;
    private final String prefixToServe;
    private final CompressionStrategy compressionStrategy;
    private final Path compressedFilesFolder;
    private final Map<Path, Object> locks = new ConcurrentHashMap<>();

    /**
     * Creates a new file serving module that sends files from the specified folder to the client if the request starts with the specified prefix.
     *
     * @param baseDirToServe The directory to serve files from.
     * @param prefixToServe The prefix to serve, e.g. <code>files/</code> to serve the file <code>text.txt</code> from <code>"/files/text.txt"</code>
     */
    public FileServerModule(Path baseDirToServe, String prefixToServe, CompressionStrategy compressionStrategy, String webserviceName) {
        this.compressionStrategy = compressionStrategy;
        if (!Files.isDirectory(baseDirToServe)) throw new IllegalArgumentException("Base dir is not a directory!");
        this.baseDirToServe = baseDirToServe;
        this.prefixToServe = prefixToServe.startsWith("/") ? prefixToServe : "/" + prefixToServe;
        if (this.compressionStrategy.compress) {
            String baseFolder = Objects.requireNonNull(System.getProperty("java.io.tmpdir"));
            compressedFilesFolder = Paths.get(baseFolder, "webservices", webserviceName, prefixToServe);
        } else {
            compressedFilesFolder = null; // unused
        }
        if (this.compressionStrategy.compressAheadOfTime) {
            assert compressedFilesFolder != null;
            AtomicInteger processedFiles = new AtomicInteger(0);
            BrotliCompressor brotli = BrotliLoader.getBrotliCompressor();
            try (Stream<Path> stream = Files.walk(baseDirToServe)) {
                stream.forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (compressionStrategy.shouldCompress(fileName) && Files.isRegularFile(path)) {
                        Path inTmpGz = compressedFilesFolder.resolve(path + ".gz");
                        Path inTmpBr = compressedFilesFolder.resolve(path + ".br");
                        try {
                            Files.createDirectories(inTmpGz.getParent());
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
                            LOGGER.warn("{}: already compressed {} files!", baseDirToServe, i);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to precompress files!", e);
            }
        }
    }

    private static void compress(Path src, FileTime srcLastModified, Path target) throws IOException {
        if (!Files.exists(target)) {
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
            target.toFile().deleteOnExit();
        }
        try (InputStream inputStream = Files.newInputStream(src);
             OutputStream brOutputStream = compressor.getCompressingOutputStream(target)) {
            inputStream.transferTo(brOutputStream);
        }
        Files.setLastModifiedTime(target, srcLastModified);
    }

    public ResponseToken serveFileIfHandled(HTTPRequest request, ResponseStartWriter writer) throws HTTPWriteException {
        String path = request.getPath();
        if (path.startsWith(prefixToServe)) {
            String subPath = path.substring(prefixToServe.length());
            Path resolved = baseDirToServe.resolve(subPath).toAbsolutePath();
            boolean invalid = subPath.startsWith("/") || subPath.contains("..") || resolved.startsWith(baseDirToServe);
            if (invalid) {
                return writer.respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Invalid path");
            }

            if (subPath.isBlank() || !Files.isRegularFile(resolved)) {
                return null;
            }

            Method method = request.getMethod();
            if (method == Method.OPTIONS) {
                return writer.respondWithoutContentType(Status.NO_CONTENT)
                        .addHeader(ALLOW_HEADER)
                        .sendWithoutBody();
            }
            if (method != Method.GET && method != Method.HEAD) {
                return writer.respond(Status.METHOD_NOT_ALLOWED, CommonContentTypes.PLAIN)
                        .addHeader(ALLOW_HEADER)
                        .writeBodyAndFlush("Only GET and HEAD are supported for files!");
            }

            FileTime srcLastModifiedTime;
            try {
                srcLastModifiedTime = Files.getLastModifiedTime(resolved);
            } catch (IOException e) {
                LOGGER.error("Failed to read last modified time {} for path {}", resolved, path, e);
                return writer.respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN)
                        .writeBodyAndFlush("An error occurred while reading the file");
            }
            String ifModifiedSince = request.getSingleHeader("if-modified-since");
            String lastModified = ResponseDateFormatter.format(LocalDateTime.ofInstant(srcLastModifiedTime.toInstant(), GMT));
            if (ifModifiedSince != null && ifModifiedSince.equals(lastModified)) {
                // See https://httpwg.org/specs/rfc9110.html#status.304
                return writer.respondWithoutContentType(Status.NOT_MODIFIED).sendWithoutBody();
            }


            String mime = URLConnection.guessContentTypeFromName(resolved.getFileName().toString());
            if (mime == null) mime = CommonContentTypes.BINARY_DATA.mimeType; // use this as a catch-all for unknown types

            Path inCompressed = null;
            String allowedEncodings = request.getSingleHeader("accept-encoding");
            boolean shouldBrCompress = false;
            boolean shouldGzCompress = false;
            if (allowedEncodings != null && compressionStrategy.compress && compressionStrategy.shouldCompress(subPath)) {
                shouldBrCompress = allowedEncodings.contains("br");
                shouldGzCompress = allowedEncodings.contains("gzip");
                BrotliCompressor brotli = null;
                if (shouldBrCompress) {
                    brotli = BrotliLoader.getBrotliCompressor();
                    if (brotli == null) shouldBrCompress = false;
                }
                if (shouldGzCompress || shouldBrCompress) {
                    inCompressed = compressedFilesFolder.resolve(subPath + (brotli != null ? ".br" : ".gz"));
                    try {
                        if (!Files.exists(inCompressed) || (compressionStrategy.validateStored && !Files.getLastModifiedTime(inCompressed).equals(srcLastModifiedTime))) {
                            Object lock = locks.computeIfAbsent(inCompressed, ignored -> new Object());
                            synchronized (lock) {
                                // evaluate the condition again, maybe someone else already compressed while we waited for the lock
                                if (!Files.exists(inCompressed) || (compressionStrategy.validateStored && !Files.getLastModifiedTime(inCompressed).equals(srcLastModifiedTime))) {
                                    if (brotli != null) {
                                        compressBrotli(resolved, brotli, srcLastModifiedTime, inCompressed);
                                    } else {
                                        compress(resolved, srcLastModifiedTime, inCompressed);
                                    }
                                    locks.remove(inCompressed);
                                }
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.warn("Failed to compress file {}", resolved);
                        inCompressed = null;
                    }
                }
            }

            ResponseHeaderWriter headerWriter = null;
            try (FileChannel channel = FileChannel.open(inCompressed == null ? resolved : inCompressed)) {
                long size = channel.size();
                headerWriter = writer.respond(Status.OK, mime);
                if (inCompressed != null) {
                    headerWriter.addHeader(shouldBrCompress ? CONTENT_ENCODING_BROTLI : CONTENT_ENCODING_GZIP);
                }
                headerWriter.addHeader(LAST_MODIFIED, lastModified);

                if (method == Method.HEAD) {
                    return headerWriter.addHeader(BuiltinHeaders.CONTENT_LENGTH.headerKey, size + "").sendWithoutBody();
                } else {
                    FixedResponseBodyWriter bodyWriter = headerWriter.beginBodyWithKnownSize(size);
                    channel.transferTo(0, size, bodyWriter.getRawSocketChannel());
                    return bodyWriter.finalizeResponse();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to serve file {} for path {}", resolved, path, e);
                if (headerWriter != null) {
                    try {
                        writer = headerWriter.resetResponseBuilder();
                    } catch (IllegalStateException e1) {
                        // Shoot. Can't reset, it's too late...
                        throw new HTTPWriteException(e);
                    }
                }
                return writer.respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN)
                        .writeBodyAndFlush("An error occurred while reading the file");
            }
        }
        return null;
    }
}
