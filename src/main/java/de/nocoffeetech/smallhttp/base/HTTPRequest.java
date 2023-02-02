package de.nocoffeetech.smallhttp.base;

import de.nocoffeetech.smallhttp.data.HTTPVersion;
import de.nocoffeetech.smallhttp.data.Method;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface HTTPRequest {
    HTTPVersion getVersion();

    Method getMethod();

    String getPath();

    Map<String, String> getQueryParameters();

    String getSingleHeader(String key);

    List<String> getHeaders(String key);

    InputStream getInputStream();

    int getContentLength();
}
