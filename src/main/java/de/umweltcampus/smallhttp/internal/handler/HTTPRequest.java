package de.umweltcampus.smallhttp.internal.handler;

import de.umweltcampus.smallhttp.data.HTTPVersion;
import de.umweltcampus.smallhttp.data.Method;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HTTPRequest {
    private final Method method;
    private final URLParser urlParser;
    private final String path;
    private final HTTPVersion version;
    private final Map<String, Object> headers; // Value: Either String or List<String>
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

    @SuppressWarnings("unchecked")
    void addHeader(String name, String value) {
        assert name != null && !name.isBlank();
        assert value != null;
        // Headers are case-insensitive, so lowercase them
        String key = name.toLowerCase(Locale.ROOT);
        Object presentVal = headers.putIfAbsent(key, value);
        // yes, check by reference, and not using equals on a string is correct here
        if (presentVal != null) {
            if (presentVal instanceof List<?>) {
                List<String> list = (List<String>) presentVal;
                list.add(value);
            } else {
                List<String> list = new ArrayList<>();
                list.add((String) presentVal);
                list.add(value);
                headers.put(key, list);
            }
        }
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
     * @return The value of the header, or null if this header is absent or multiple value are present
     */
    public String getSingleHeader(String key) {
        assert key.toLowerCase(Locale.ROOT).equals(key);
        Object entry = headers.get(key);
        if (entry instanceof String) return (String) entry;
        return null;
    }

    /**
     * Gets the headers for a given key.
     * @param key The lowercase key of the header
     * @return The values of that header, or null if no such header is present
     */
    @SuppressWarnings("unchecked")
    public List<String> getHeaders(String key) {
        assert key.toLowerCase(Locale.ROOT).equals(key);
        Object o = headers.get(key);
        if (o == null) return null;
        else if (o instanceof List<?>) return (List<String>) o;
        else return Collections.singletonList((String) o);
    }

    public RestBufInputStream getInputStream() {
        return restBufInputStream;
    }

    boolean isAsteriskRequest() {
        return urlParser.isAsteriskRequest();
    }
}
