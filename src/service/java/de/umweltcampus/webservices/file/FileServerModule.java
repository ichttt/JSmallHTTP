package de.umweltcampus.webservices.file;

import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.BuiltinHeaders;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeader;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import de.umweltcampus.smallhttp.internal.handler.HTTPRequest;
import de.umweltcampus.smallhttp.response.FixedResponseBodyWriter;
import de.umweltcampus.smallhttp.response.HTTPWriteException;
import de.umweltcampus.smallhttp.response.ResponseHeaderWriter;
import de.umweltcampus.smallhttp.response.ResponseStartWriter;
import de.umweltcampus.smallhttp.response.ResponseToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileServerModule {
    private static final Logger LOGGER = LogManager.getLogger(FileServerModule.class);
    private static final PrecomputedHeader ALLOW_HEADER = new PrecomputedHeader(new PrecomputedHeaderKey("Allow"), Stream.of(Method.OPTIONS, Method.GET, Method.HEAD).map(Enum::name).collect(Collectors.joining(", ")));
    private final Path baseDirToServe;
    private final String prefixToServe;

    /**
     * Creates a new file serving module that sends files from the specified folder to the client if the request starts with the specified prefix.
     *
     * @param baseDirToServe The directory to serve files from.
     * @param prefixToServe The prefix to serve, e.g. <code>files/</code> to serve the file <code>text.txt</code> from <code>"/files/text.txt"</code>
     */
    public FileServerModule(Path baseDirToServe, String prefixToServe) {
        if (!Files.isDirectory(baseDirToServe)) throw new IllegalArgumentException("Base dir is not a directory!");
        this.baseDirToServe = baseDirToServe;
        this.prefixToServe = prefixToServe.startsWith("/") ? prefixToServe : "/" + prefixToServe;
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

            String mime = URLConnection.guessContentTypeFromName(resolved.getFileName().toString());
            if (mime == null) mime = CommonContentTypes.BINARY_DATA.mimeType; // use this as a catch-all for unknown types

            ResponseHeaderWriter headerWriter = null;
            try (FileChannel channel = FileChannel.open(resolved)) {
                long size = channel.size();
                if (method == Method.HEAD) {
                    return writer.respond(Status.OK, mime).addHeader(BuiltinHeaders.CONTENT_LENGTH.headerKey, size + "").sendWithoutBody();
                } else {
                    headerWriter = writer.respond(Status.OK, mime);
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
