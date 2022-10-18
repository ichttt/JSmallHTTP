package de.umweltcampus.smallhttp.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.response.ResponseBodyWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ResponseWriterTest {

    @Test
    public void testEmptyHeaderWriting() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(), HTTPVersion.HTTP_1_1);
        writer.respond(Status.OK)
                .beginBodyWithUnknownSize()
                .finalizeResponse();

        // TODO once builtin headers exists, these need to be incorporated
        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 200 Ok\r\n\r\n", s);
    }

    @Test
    public void testSingleHeaderWriting() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(), HTTPVersion.HTTP_1_1);
        writer.respond(Status.OK)
                .addHeader("Server", "ABCTest")
                .beginBodyWithUnknownSize()
                .finalizeResponse();

        // TODO once builtin headers exists, these need to be incorporated
        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 200 Ok\r\nServer:ABCTest\r\n\r\n", s);
    }

    @Test
    public void testBodyWriting() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(), HTTPVersion.HTTP_1_1);
        ResponseBodyWriter responseBodyWriter = writer.respond(Status.OK)
                .addHeader("Server", "ABCTest")
                .beginBodyWithUnknownSize();
        responseBodyWriter.writeString("Das ist ein Test!");
        responseBodyWriter.finalizeResponse();

        // TODO once builtin headers exists, these need to be incorporated
        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 200 Ok\r\nServer:ABCTest\r\n\r\nDas ist ein Test!", s);
    }

    @Test
    public void testBuilderReset() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(), HTTPVersion.HTTP_1_1);
        ResponseBodyWriter responseBodyWriter = writer.respond(Status.OK)
                .addHeader("Server", "ABCTest")
                .resetResponseBuilder()
                .respond(Status.INTERNAL_SERVER_ERROR)
                .addHeader("Server", "Test2")
                .beginBodyWithUnknownSize();
        responseBodyWriter.writeString("Das ist ein Test!");
        responseBodyWriter.finalizeResponse();

        // TODO once builtin headers exists, these need to be incorporated
        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 500 Internal Server Error\r\nServer:Test2\r\n\r\nDas ist ein Test!", s);
    }
}
