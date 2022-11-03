package de.umweltcampus.smallhttp.internal.handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class URLParser {
    private final byte[] buf;
    private final int start;
    private final int end;
    private int queryIndex;
    private URL url;
    private Map<String, String> map;
    private boolean parsedTarget = false;

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
            int fragmentIndex = searchNext(start, end, '#');
            int indexToUse;
            if (fragmentIndex != -1 && (queryIndex > fragmentIndex || queryIndex == -1)) {
                this.queryIndex = -1;
                indexToUse = fragmentIndex;
            } else {
                this.queryIndex = queryIndex;
                indexToUse = queryIndex;
            }
            int length = (indexToUse == -1 ? end : indexToUse) - start;
            parsedTarget = true;
            return URLDecoder.decode(new String(buf, start, length), StandardCharsets.UTF_8);
        } else if (end > start + 4  && buf[start] == 'h' && buf[start + 1] == 't' && buf[start + 2] == 't' && buf[start + 3] == 'p') {
            // most likely https://www.rfc-editor.org/rfc/rfc9112#name-absolute-form
            // leverage the standard URL parser for this
            try {
                url = new URL(new String(buf, start, end - start));
                parsedTarget = true;
                return url.getPath();
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
        if (!parsedTarget) throw new IllegalStateException();

        String query;
        if (url != null) {
            query = url.getQuery();
            if (query == null) query = "";
        } else if (queryIndex != -1) {
            int startIndex = queryIndex + 1;
            int length = end - startIndex;
            if (length > 0) {
                query = new String(buf, startIndex, length);
            } else {
                query = "";
            }
        } else {
            query = "";
        }
        if (query.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new LinkedHashMap<>();

        // See https://www.rfc-editor.org/rfc/rfc3986#section-3.4
        String currKey = null;
        int nextStart = 0;
        int length = query.length();
        for (int i = 0; i < length; i++) {
            char c = query.charAt(i);
            if (currKey == null && c == '=') {
                currKey = URLDecoder.decode(query.substring(nextStart, i), StandardCharsets.UTF_8);
                nextStart = i + 1;
            } else if (c == '&') {
                String val = URLDecoder.decode(query.substring(nextStart, i), StandardCharsets.UTF_8);
                if (currKey != null) {
                    map.put(currKey, val);
                    currKey = null;
                } else {
                    map.put(val, null);
                }
                nextStart = i + 1;
            } else if (c == '#' || i == length - 1) {
                String subString = c == '#' ? query.substring(nextStart, i) : query.substring(nextStart);
                String val = URLDecoder.decode(subString, StandardCharsets.UTF_8);
                if (currKey != null) {
                    map.put(currKey, val);
                } else if (!val.isEmpty()) {
                    map.put(val, null);
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
