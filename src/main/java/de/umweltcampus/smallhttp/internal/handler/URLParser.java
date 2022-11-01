package de.umweltcampus.smallhttp.internal.handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class URLParser {
    private final byte[] buf;
    private final int start;
    private final int end;
    private int queryIndex;
    private URL url;
    private Map<String, String> map;

    public URLParser(byte[] buf, int start, int end) {
        this.buf = buf;
        this.start = start;
        this.end = end;
    }

    public String parseRequestTarget() {
        // https://www.rfc-editor.org/rfc/rfc9112#name-request-target
        if (buf[start] == '/') {
            // https://www.rfc-editor.org/rfc/rfc9112#name-origin-form
            // search the query split
            int queryIndex = searchNext(start, end, '?');
            this.queryIndex = queryIndex;
            int length = (queryIndex == -1 ? end : queryIndex) - start;
            return URLDecoder.decode(new String(buf, start, length), StandardCharsets.UTF_8);
        } else if (start + 4 > end && buf[start] == 'h' && buf[start + 1] == 't' && buf[start + 2] == 't' && buf[start + 3] == 'p') {
            // most likely https://www.rfc-editor.org/rfc/rfc9112#name-absolute-form
            // leverage the standard URL parser for this
            try {
                url = new URL(new String(buf, start, end - start));
                return url.getFile();
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return null;
    }

    public Map<String, String> parseOrGetQuery() {
        Map<String, String> map = this.map;
        if (map == null)
            map = this.map = parseQuery();
        return map;
    }

    private Map<String, String> parseQuery() {
        String query;
        if (url != null) {
            query = url.getQuery();
            if (query == null) query = "";
        } else if (queryIndex != -1) {
            query = new String(buf, queryIndex, end - queryIndex);
        } else {
            query = "";
        }
        if (query.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new LinkedHashMap<>();

        // See https://www.rfc-editor.org/rfc/rfc3986#section-3.4
        String currKey = null;
        int lastStart = 0;
        int length = query.length();
        for (int i = 0; i < length; i++) {
            char c = query.charAt(i);
            if (currKey == null && c == '=') {
                currKey = URLDecoder.decode(query.substring(lastStart + 1, i), StandardCharsets.UTF_8);
                lastStart = i;
            } else if (c == '&') {
                String val = URLDecoder.decode(query.substring(lastStart + 1, i), StandardCharsets.UTF_8);
                if (currKey != null) {
                    map.put(currKey, val);
                    currKey = null;
                } else {
                    map.put(val, "");
                }
                lastStart = i;
            } else if (c == '#' || i == length - 1) {
                String val = URLDecoder.decode(query.substring(lastStart + 1), StandardCharsets.UTF_8);
                if (currKey != null) {
                    map.put(currKey, val);
                } else {
                    map.put(val, "");
                }
                break;
            }
        }
        return map;
    }

    private int searchNext(int start, int max, char charToFind) {
        byte[] toSearch = buf;
        for (int i = start; i < max; i++) {
            if (toSearch[i] == charToFind)
                return i;
        }
        return -1;
    }
}
