package de.umweltcampus.smallhttp.header;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class PrecomputedHeaderKeyTest {

    @Test
    public void testPrecomputedHeaderKey() {
        String name = "Server";
        PrecomputedHeaderKey key = new PrecomputedHeaderKey(name);
        Assertions.assertEquals(name + ":", new String(key.asciiBytes, StandardCharsets.US_ASCII));
    }

    @Test
    public void testInvalidPrecomputedHeaderKey() {
        String name = "Server:";
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PrecomputedHeaderKey(name));
    }
}
