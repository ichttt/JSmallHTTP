package de.umweltcampus.webservices.builtin.config;

import java.util.Map;

public class FolderInfo {
    public final String prefixToServe;
    public final String pathOnDisk;
    public final boolean precompress;
    public final Map<String, String> additionalHeaders;

    public FolderInfo(String prefixToServe, String pathOnDisk, boolean precompress, Map<String, String> additionalHeaders) {
        this.prefixToServe = prefixToServe;
        this.pathOnDisk = pathOnDisk;
        this.precompress = precompress;
        this.additionalHeaders = additionalHeaders;
    }
}
