package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.data.Method;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.response.FixedResponseBodyWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Test {

    public static void main(String[] args) throws IOException {
        RequestHandler handler = (request, responseWriter) -> {
            Path base = Paths.get("test/");
            if (request.getPath().startsWith("/test/")) {
                Path actual = Paths.get(base.toString(), request.getPath().substring("/test/".length())).toAbsolutePath();
                if (!actual.startsWith(base.toAbsolutePath()))
                    return responseWriter.respond(Status.FORBIDDEN, CommonContentTypes.PLAIN).writeBodyAndFlush("Nope that's not allowed");
                if (!Files.exists(actual))
                    return responseWriter.respond(Status.NOT_FOUND, CommonContentTypes.PLAIN).writeBodyAndFlush("Not found");
                FileChannel channel = FileChannel.open(actual);
                long size = channel.size();
                FixedResponseBodyWriter fixedResponseBodyWriter = responseWriter.respond(Status.OK, getType(request.getPath())).beginBodyWithKnownSize((int) size);
                Channels.newInputStream(channel).transferTo(fixedResponseBodyWriter.getRawOutputStream());
                return fixedResponseBodyWriter.finalizeResponse();
            } else if (request.getPath().startsWith("/api/") && request.getMethod() == Method.PUT) {
                InputStream stream = request.getInputStream();
                if (stream == null) return responseWriter.respond(Status.LENGTH_REQUIRED, CommonContentTypes.PLAIN).writeBodyAndFlush("Supply the content length!");
                Path path;
                String name;
                do {
                    name = "test_img_" + ((int) (Math.random() * 100000)) + ".png";
                    path = Paths.get(base.toString(), name);
                } while (Files.exists(path));
                try (OutputStream oStream = Files.newOutputStream(path)) {
                    stream.transferTo(oStream);
                }
                return responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("/test/" + name);
            }
            return responseWriter.respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("nope");
        };
        HTTPServer httpServer = HTTPServerBuilder.create(8080, handler).setRequestHeaderReadTimeout(10000).build();
    }

    private static CommonContentTypes getType(String name) {
        if (name.endsWith(".html"))
            return CommonContentTypes.HTML;
        else if (name.endsWith(".js"))
            return CommonContentTypes.JS;
        else if (name.endsWith(".css"))
            return CommonContentTypes.CSS;
        else if (name.endsWith(".png"))
            return CommonContentTypes.PNG;
        else
            return CommonContentTypes.PLAIN;
    }
}
