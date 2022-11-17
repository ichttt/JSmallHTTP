package de.umweltcampus.webservices.internal.builtin;

public class FolderInfo {
    public final String prefixToServe;
    public final String pathOnDisk;
    public final boolean precompress;

    public FolderInfo(String prefixToServe, String pathOnDisk, boolean precompress) {
        this.prefixToServe = prefixToServe;
        this.pathOnDisk = pathOnDisk;
        this.precompress = precompress;
    }
}
