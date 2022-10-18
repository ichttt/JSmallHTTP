package de.umweltcampus.smallhttp.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HTTPVersionTest {

    @Test
    public void testCommonBytes() {
        for (HTTPVersion value : HTTPVersion.values()) {
            assert value.name.equals(new String(HTTPVersion.COMMON_BYTES, StandardCharsets.US_ASCII) + ((char) value.lastByte));
        }
    }

    @Test
    public void testCommonBytesLength() {
        Assertions.assertEquals(7, HTTPVersion.COMMON_BYTES.length, "Common Bytes must be 7 so at most 10 bytes after the offset get read");
    }

    @Test
    public void testValidHttp11Parsing() {
        byte[] data = "HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII);
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(data, 0);
        Assertions.assertEquals(HTTPVersion.HTTP_1_1, matchingVersion);
    }

    @Test
    public void testValidHttp10Parsing() {
        byte[] data = "HTTP/1.0\r\n".getBytes(StandardCharsets.US_ASCII);
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(data, 0);
        Assertions.assertEquals(HTTPVersion.HTTP_1_0, matchingVersion);
    }

    @Test
    public void testValidOffsetHttp11Parsing() {
        byte[] data = "GET / HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII);
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(data, 6);
        Assertions.assertEquals(HTTPVersion.HTTP_1_1, matchingVersion);
    }

    @Test
    public void testInvalidHttpName() {
        byte[] data = "ROFL/1.1\r\n".getBytes(StandardCharsets.US_ASCII);
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(data, 0);
        Assertions.assertNull(matchingVersion);
    }

    @Test
    public void testInvalidEnding() {
        byte[] data = "HTTP/1.1 \r\n".getBytes(StandardCharsets.US_ASCII);
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(data, 0);
        Assertions.assertNull(matchingVersion);
    }

    @Test
    public void testInvalidOffset() {
        byte[] data = "GET / HTTP/1.1\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
        HTTPVersion matchingVersion = HTTPVersion.findMatchingVersion(data, 7);
        Assertions.assertNull(matchingVersion);
    }

    @Test
    public void testWriteToHeader() throws IOException {
        for (HTTPVersion version : HTTPVersion.values()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            version.writeToHeader(outputStream);
            String resultingString = outputStream.toString(StandardCharsets.US_ASCII);
            Assertions.assertEquals(version.name + " ", resultingString, "Failed at version " + version);
        }
    }
}
