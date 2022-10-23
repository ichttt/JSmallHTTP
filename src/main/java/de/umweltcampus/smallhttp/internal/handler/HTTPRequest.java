package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Method;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HTTPRequest {
    private final Method method;
    private final String url;
    private final HTTPVersion version;
    private final Map<String, List<String>> headers;
    private boolean hasOpenedInputStream = false;
    private byte[] restBuffer;
    private int bufOffset;
    private int bufLength;
    private InputStream originalInputStream;

    public HTTPRequest(Method method, String url, HTTPVersion version) {
        this.method = method;
        this.url = url;
        this.version = version;
        this.headers = new HashMap<>();
    }

    void setRestBuffer(byte[] restBuffer, int bufOffset, int bufLength, InputStream originalInputStream) {
        this.restBuffer = restBuffer;
        this.bufOffset = bufOffset;
        this.bufLength = bufLength;
        this.originalInputStream = originalInputStream;
    }

    void addHeader(String name, String value) {
        assert name != null && !name.isBlank();
        assert value != null;
        // Headers are case-insensitive, so lowercase them
        headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), s -> new ArrayList<>(1)).add(value);
    }

    public HTTPVersion getVersion() {
        return version;
    }

    public Method getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Gets the first header for a given key.
     * @param key The lowercase key of the header
     * @return The first set value of that header, or null if no such header is present
     */
    public String getFirstHeader(String key) {
        List<String> strings = headers.get(key);
        if (strings == null) return null;
        return strings.get(0);
    }

    /**
     * Gets the headers for a given key.
     * @param key The lowercase key of the header
     * @return The values of that header, or null if no such header is present
     */
    public List<String> getHeaders(String key) {
        return headers.get(key);
    }

    public InputStream openInputStream() {
        // TODO validate content-length?
        if (hasOpenedInputStream) throw new IllegalStateException();
        hasOpenedInputStream = true;
        return new SequenceInputStream(new ByteArrayInputStream(restBuffer, bufOffset, bufLength), originalInputStream);
    }
}
