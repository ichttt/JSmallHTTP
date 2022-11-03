package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.response.ChunkedResponseWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws IOException {
        RequestHandler handler = (request, responseWriter) -> {
            if (request.getPath().equals("/data.html")) {
                Map<String, String> queryParameters = request.getQueryParameters();
                String data = queryParameters.get("data");
                if (data == null)
                    return responseWriter.respond(Status.BAD_REQUEST, CommonContentTypes.PLAIN).writeBodyAndFlush("Missing data!");
                else
                    return responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("You provided the following value: ", data);
            }
            if (request.getPath().equals("/test")) {
                return responseWriter.respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN).writeBodyAndFlush("No such image");
            }
            if (request.getPath().equals("/abcd")) {
                ChunkedResponseWriter chunkedResponseWriter = responseWriter.respond(Status.OK, CommonContentTypes.HTML).beginBodyWithUnknownSize();
                chunkedResponseWriter.writeFromInputStream(Files.newInputStream(Paths.get("./test.html")));
                return chunkedResponseWriter.finalizeResponse();
            }
            return responseWriter.respond(Status.OK, CommonContentTypes.HTML).writeBodyAndFlush("<html><body><img src=\"/test\"/></body></html>");
        };
        HTTPServer httpServer = HTTPServerBuilder.create(8080, handler).setRequestHeaderReadTimeout(10000).build();
    }
}
