package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Status;
import de.umweltcampus.smallhttp.header.CommonContentTypes;
import de.umweltcampus.smallhttp.header.PrecomputedHeaderKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ResponseWriterTest {
    private static final PrecomputedHeaderKey ETAG = new PrecomputedHeaderKey("ETag");

    @Test
    public void testEmptyHeaderWriting() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(TestDateFormatter.INSTANCE), HTTPVersion.HTTP_1_1);
        writer.respond(Status.OK, CommonContentTypes.PLAIN)
                .beginBodyWithKnownSize(0)
                .finalizeResponse();

        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 200 Ok\r\nDate:" + TestDateFormatter.EXPECTED_VAL + "\r\nServer:JSmallHTTP\r\nContent-Type:text/plain;charset=UTF-8\r\nContent-Length:0\r\n\r\n", s);
    }

    @Test
    public void testSingleHeaderWriting() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(TestDateFormatter.INSTANCE), HTTPVersion.HTTP_1_1);
        writer.respond(Status.OK, CommonContentTypes.PLAIN)
                .addHeader(ETAG, "ABCTest")
                .beginBodyWithKnownSize(0)
                .finalizeResponse();

        // TODO once builtin headers exists, these need to be incorporated
        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 200 Ok\r\nDate:" + TestDateFormatter.EXPECTED_VAL + "\r\nServer:JSmallHTTP\r\nContent-Type:text/plain;charset=UTF-8\r\nETag:ABCTest\r\nContent-Length:0\r\n\r\n", s);
    }

    @Test
    public void testBodyWriting() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(TestDateFormatter.INSTANCE), HTTPVersion.HTTP_1_1);
        writer.respond(Status.OK, CommonContentTypes.PLAIN)
                .addHeader(ETAG, "ABCTest")
                .writeBodyAndFlush("Das ist ein Test!");

        // TODO once builtin headers exists, these need to be incorporated
        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 200 Ok\r\nDate:" + TestDateFormatter.EXPECTED_VAL + "\r\nServer:JSmallHTTP\r\nContent-Type:text/plain;charset=UTF-8\r\nETag:ABCTest\r\nContent-Length:17\r\n\r\nDas ist ein Test!", s);
    }

    @Test
    public void testBuilderReset() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ResponseWriter writer = new ResponseWriter(outputStream, new ReusableClientContext(TestDateFormatter.INSTANCE), HTTPVersion.HTTP_1_1);
        writer.respond(Status.OK, CommonContentTypes.PLAIN)
                .addHeader(ETAG, "ABCTest")
                .resetResponseBuilder()
                .respond(Status.INTERNAL_SERVER_ERROR, CommonContentTypes.PLAIN)
                .addHeader(ETAG, "Test2")
                .writeBodyAndFlush("Das ist ein Test!");

        // TODO once builtin headers exists, these need to be incorporated
        String s = outputStream.toString(StandardCharsets.US_ASCII);
        Assertions.assertEquals("HTTP/1.1 500 Internal Server Error\r\nDate:" + TestDateFormatter.EXPECTED_VAL + "\r\nServer:JSmallHTTP\r\nContent-Type:text/plain;charset=UTF-8\r\nETag:Test2\r\nContent-Length:17\r\n\r\nDas ist ein Test!", s);
    }
}
