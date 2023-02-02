package de.nocoffeetech.webservices.file;

import de.nocoffeetech.webservices.file.compress.CompressionStrategy;
import de.nocoffeetech.webservices.file.compress.FileCompressor;
import de.nocoffeetech.webservices.service.WebserviceBase;
import de.nocoffeetech.smallhttp.base.HTTPRequest;
import de.nocoffeetech.smallhttp.data.Method;
import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.header.BuiltinHeaders;
import de.nocoffeetech.smallhttp.header.CommonContentTypes;
import de.nocoffeetech.smallhttp.header.PrecomputedHeader;
import de.nocoffeetech.smallhttp.header.PrecomputedHeaderKey;
import de.nocoffeetech.smallhttp.response.FixedResponseBodyWriter;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseHeaderWriter;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;
import de.nocoffeetech.smallhttp.util.ResponseDateFormatter;
import de.nocoffeetech.webservices.internal.util.TempDirHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileServerModule {
    private static final Logger LOGGER = LogManager.getLogger(FileServerModule.class);
    private static final PrecomputedHeader ALLOW_HEADER = PrecomputedHeader.create("Allow", Stream.of(Method.OPTIONS, Method.GET, Method.HEAD).map(Enum::name).collect(Collectors.joining(", ")));
    private static final PrecomputedHeaderKey LAST_MODIFIED = PrecomputedHeaderKey.create("Last-Modified");
    private static final ZoneId GMT = ZoneId.of("GMT");
    protected final Path baseDirToServe;
    protected final String prefixToServe;
    protected final FileCompressor compressor;
    protected final BiConsumer<HTTPRequest, ResponseHeaderWriter> additionalHeaderAdder;

    /**
     * Creates a new file serving module that sends files from the specified folder to the client if the request starts with the specified prefix.
     *
     * @param baseDirToServe The directory to serve files from.
     * @param prefixToServe The prefix to serve, e.g. <code>files/</code> to serve the file <code>text.txt</code> from <code>"/files/text.txt"</code>
     * @param compressionStrategy The strategy that tells the module how to handle compression for different files
     * @param responsibleWebservice The webservice this module is being created for
     * @param additionalHeaderAdder A consumer that allows the webservice to add additional headers such as Cache-Control
     */
    public FileServerModule(Path baseDirToServe, String prefixToServe, CompressionStrategy compressionStrategy, WebserviceBase responsibleWebservice, BiConsumer<HTTPRequest, ResponseHeaderWriter> additionalHeaderAdder) {
        this.additionalHeaderAdder = additionalHeaderAdder;
        if (!Files.isDirectory(baseDirToServe)) throw new IllegalArgumentException("Base dir is not a directory!");
        this.baseDirToServe = baseDirToServe.toAbsolutePath();
        this.prefixToServe = prefixToServe.startsWith("/") ? prefixToServe : "/" + prefixToServe;
        if (compressionStrategy.compress) {
            Path compressedFilesFolder = TempDirHelper.createTempPathFor(responsibleWebservice.getName(), prefixToServe);
            compressor = new FileCompressor(compressionStrategy, baseDirToServe, compressedFilesFolder);
        } else {
            compressor = null; // unused
        }
        if (compressionStrategy.compressAheadOfTime) {
            assert compressor != null;
            compressor.precompress();
        }
    }

    public boolean isHandled(HTTPRequest request) {
        return request.getPath().startsWith(prefixToServe);
    }

    public ResponseToken serveFile(HTTPRequest request, ResponseStartWriter writer) throws HTTPWriteException {
        String subPath = request.getPath().substring(prefixToServe.length());
        Path resolved = baseDirToServe.resolve(subPath).toAbsolutePath();
        boolean invalid = subPath.startsWith("/") || subPath.contains("..") || !resolved.startsWith(baseDirToServe);
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

        return serveValidFile(request, writer, subPath, resolved, method);
    }

    protected ResponseToken serveValidFile(HTTPRequest request, ResponseStartWriter writer, String subPath, Path toServe, Method method) throws HTTPWriteException {
        FileTime srcLastModifiedTime;
        try {
            srcLastModifiedTime = Files.getLastModifiedTime(toServe);
        } catch (IOException e) {
            LOGGER.error("Failed to read last modified time {}", toServe, e);
            return writer.respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN)
                    .writeBodyAndFlush("An error occurred while reading the file");
        }
        String ifModifiedSince = request.getSingleHeader("if-modified-since");
        String lastModified = ResponseDateFormatter.format(LocalDateTime.ofInstant(srcLastModifiedTime.toInstant(), GMT));
        if (ifModifiedSince != null && ifModifiedSince.equals(lastModified)) {
            // See https://httpwg.org/specs/rfc9110.html#status.304
            return writer.respondWithoutContentType(Status.NOT_MODIFIED).sendWithoutBody();
        }

        String mime = URLConnection.guessContentTypeFromName(toServe.getFileName().toString());
        if (mime == null) mime = CommonContentTypes.BINARY_DATA.mimeType; // use this as a catch-all for unknown types

        ResponseHeaderWriter headerWriter = writer.respond(Status.OK, mime);

        headerWriter.addHeader(LAST_MODIFIED, lastModified);

        String allowedEncodings = request.getSingleHeader("accept-encoding");
        if (compressor != null) {
            Path pathInCompressed = compressor.getPathInCompressed(allowedEncodings, subPath, toServe, srcLastModifiedTime, headerWriter);
            if (pathInCompressed != null) {
                toServe = pathInCompressed;
            }
        }
        if (this.additionalHeaderAdder != null) {
            this.additionalHeaderAdder.accept(request, headerWriter);
        }

        return sendFile(toServe, method, headerWriter);
    }

    public static ResponseToken sendFile(Path toSend, Method requestMethod, ResponseHeaderWriter headerWriter) throws HTTPWriteException {
        try (FileChannel channel = FileChannel.open(toSend)) {
            long size = channel.size();

            if (requestMethod == Method.HEAD) {
                return headerWriter.addHeader(BuiltinHeaders.CONTENT_LENGTH.headerKey, size + "").sendWithoutBody();
            } else {
                FixedResponseBodyWriter bodyWriter = headerWriter.beginBodyWithKnownSize(size);
                channel.transferTo(0, size, bodyWriter.getRawSocketChannel());
                return bodyWriter.finalizeResponse();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to serve file {}!", toSend, e);
            if (!headerWriter.canResetResponseWriter()) {
                // Shoot. Can't reset, it's too late...
                throw new HTTPWriteException(e);
            }
            return headerWriter
                    .resetResponseBuilder()
                    .respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN)
                    .writeBodyAndFlush("An error occurred while reading the file");
        }
    }
}
