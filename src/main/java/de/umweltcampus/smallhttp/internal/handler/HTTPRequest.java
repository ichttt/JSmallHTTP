package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Method;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HTTPRequest {
    private final Method method;
    private final URLParser urlParser;
    private final String path;
    private final HTTPVersion version;
    private final Map<String, List<String>> headers;
    private RestBufInputStream restBufInputStream;

    public HTTPRequest(Method method, URLParser parser, HTTPVersion version) {
        this.method = method;
        this.urlParser = parser;
        this.path = parser.parseRequestTarget();
        this.version = version;
        this.headers = new HashMap<>();
    }

    void setRestBuffer(byte[] restBuffer, int bufOffset, int bufLength, InputStream originalInputStream, int contentLength) {
        this.restBufInputStream = new RestBufInputStream(restBuffer, bufOffset, bufLength, originalInputStream, contentLength);
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

    public String getPath() {
        return path;
    }

    /**
     * Gets the URL query parameters, parsing them on the first call to this method
     * @return A map with the key-value pairs of the parameters. The order of items is preserved
     */
    public Map<String, String> getQueryParameters() {
        return urlParser.parseOrGetQuery();
    }

    /**
     * Gets the first header for a given key.
     * @param key The lowercase key of the header
     * @return The first set value of that header, or null if no such header is present
     */
    public String getFirstHeader(String key) {
        assert key.toLowerCase(Locale.ROOT).equals(key);
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
        assert key.toLowerCase(Locale.ROOT).equals(key);
        return headers.get(key);
    }

    public RestBufInputStream getInputStream() {
        return restBufInputStream;
    }

    boolean isAsteriskRequest() {
        return urlParser.isAsteriskRequest();
    }
}
