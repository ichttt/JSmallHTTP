package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException {
        HTTPServer httpServer = new HTTPServer(8080, (request, responseWriter) -> {
            if (request.getUrl().equals("/test")) {
                return responseWriter.respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN).writeBodyAndFlush("No such image");
            }
            return responseWriter.respond(Status.OK, CommonContentTypes.HTML).writeBodyAndFlush("<html><body><img src=\"/test\"/></body></html>");
        });
    }
}
