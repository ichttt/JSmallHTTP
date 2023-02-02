package de.nocoffeetech.smallhttp.header;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class PrecomputedHeaderTest {

    @Test
    public void testPrecomputedHeader() {
        String name = "Server";
        PrecomputedHeaderKey key = PrecomputedHeaderKey.create(name);
        String value = "JSmallHTTP";
        PrecomputedHeader precomputedHeader = PrecomputedHeader.create(key, value);
        Assertions.assertEquals(name + ":" + value + "\r\n", new String(precomputedHeader.asciiBytes, StandardCharsets.US_ASCII));
    }

    @Test
    public void testInvalidPrecomputedHeader() {
        String name = "Server";
        PrecomputedHeaderKey key = PrecomputedHeaderKey.create(name);
        String value = "JSmallHTTP\r\n";
        Assertions.assertThrows(IllegalArgumentException.class, () -> PrecomputedHeader.create(key, value));
    }

}
