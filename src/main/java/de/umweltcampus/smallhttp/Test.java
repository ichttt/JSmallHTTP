package de.umweltcampus.smallhttp;

import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException {
        HTTPServer httpServer = new HTTPServer(8080, (request, responseWriter) -> responseWriter.respond(Status.OK, CommonContentTypes.PLAIN).writeBodyAndFlush("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Proin nibh nisl condimentum id. Ipsum nunc aliquet bibendum enim facilisis gravida neque convallis. Adipiscing at in tellus integer. Tempor orci eu lobortis elementum nibh tellus molestie nunc non. Posuere urna nec tincidunt praesent. Pellentesque habitant morbi tristique senectus et netus et. Non diam phasellus vestibulum lorem. Metus vulputate eu scelerisque felis imperdiet. Tortor at risus viverra adipiscing at."));
    }
}
