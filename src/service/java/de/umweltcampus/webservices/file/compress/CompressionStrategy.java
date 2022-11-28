package de.umweltcampus.webservices.file.compress;

/**
 * Defines a strategy that determines how files are compressed before they are sent over the wire
 */
public class CompressionStrategy {
    private static final String[] DEFAULT_FILE_ENDINGS_TO_USE = new String[] {".txt", ".html", ".js", ".css"};

    public final boolean compress;
    public final boolean validateStored;
    public final boolean compressAheadOfTime;
    private final String[] fileEndingsToCompress;

    private CompressionStrategy(boolean compress, boolean validateStored, boolean compressAheadOfTime, String[] fileEndingsToCompress) {
        this.compress = compress;
        this.validateStored = validateStored;
        this.compressAheadOfTime = compressAheadOfTime;
        this.fileEndingsToCompress = fileEndingsToCompress == null || fileEndingsToCompress.length == 0 ? DEFAULT_FILE_ENDINGS_TO_USE : fileEndingsToCompress;
    }

    public boolean shouldCompress(String fileName) {
        for (String endingsToCompress : fileEndingsToCompress) {
            if (fileName.endsWith(endingsToCompress)) return true;
        }
        return false;
    }

    /**
     * Returns a new compression strategy that does not compress any file at all
     * @return The new no compression strategy
     */
    public static CompressionStrategy noCompression() {
        return new CompressionStrategy(false, false, false, null);
    }

    /**
     * Returns a new compression strategy that will store the compressed files to avoid compressing them multiple times
     * @param validateStored If true, compressed files will be validated to check if their last-modified date matches the current date. If they differ, the file is recompressed.
     * @param compressAheadOfTime If true, the module scans all files in the given folder and compress them ahead-of-time. Only use this if the folder does not have too many files. If false, files will be compressed and stored as requested
     * @param fileEndingsToCompress Which file endings to compress. Keep empty to use the default filter
     * @return The new compression strategy
     */
    public static CompressionStrategy compressAndStore(boolean validateStored, boolean compressAheadOfTime, String... fileEndingsToCompress) {
        return new CompressionStrategy(true, validateStored, compressAheadOfTime, fileEndingsToCompress);
    }
}
