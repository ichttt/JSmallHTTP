package de.umweltcampus.webservices.file;

/**
 * Defines a strategy that determines how files are compressed before they are sent over the wire
 */
public class CompressionStrategy {
    public enum CompressionMode {
        NO_COMPRESSION, ON_THE_FLY, PRECOMPRESS_NO_VALIDATION, PRECOMPRESS_VALIDATE;
    }
    private static final String[] DEFAULT_FILE_ENDINGS_TO_USE = new String[] {".txt", ".html", ".js", ".css"};

    private final CompressionMode mode;
    private final int maxSizeBytes;
    private final String[] fileEndingsToCompress;

    private CompressionStrategy(CompressionMode mode, int maxSizeBytes, String[] fileEndingsToCompress) {
        this.mode = mode;
        this.maxSizeBytes = maxSizeBytes;
        this.fileEndingsToCompress = fileEndingsToCompress == null ? DEFAULT_FILE_ENDINGS_TO_USE : fileEndingsToCompress;
    }

    /**
     * Returns a new compression strategy that does not compress any file at all
     * @return The new no compression strategy
     */
    public static CompressionStrategy noCompression() {
        return new CompressionStrategy(CompressionMode.NO_COMPRESSION, -1, null);
    }

    /**
     * Returns a new compression strategy that compresses all text files while reading them from disk.
     * This is the most CPU-intensive option, but may be appropriate for files that can be compressed well but change often
     * @param fileEndingsToCompress Which file endings to compress
     * @return The new on-the-fly compression strategy
     */
    public static CompressionStrategy compressOnTheFly(String... fileEndingsToCompress) {
        return new CompressionStrategy(CompressionMode.ON_THE_FLY, -1, fileEndingsToCompress);
    }

    /**
     * Returns a new compression strategy that compresses all text files at service startup, using them whenever possible without validation compressed files are up-to-date
     * Use this if the files are static and won't change while the server is running
     * @param maxSizeBytes The maximum file size to compress. Larger files will be served with on-the-fly compression. Use -1 to compress all files.
     * @param fileEndingsToCompress Which file endings to compress
     * @return The new compression strategy
     */
    public static CompressionStrategy precompressWithoutValidation(int maxSizeBytes, String... fileEndingsToCompress) {
        return new CompressionStrategy(CompressionMode.PRECOMPRESS_NO_VALIDATION, maxSizeBytes, fileEndingsToCompress);
    }

    /**
     * Returns a new compression strategy that compresses all text files at service startup, using them if their last-modified date matches the precompressed date.
     * If the date is different, the file will be recompressed and stored.
     * Use this if the files are static and won't change while the server is running
     * @param maxSizeBytes The maximum file size to compress. Larger files will be served with on-the-fly compression. Use -1 to compress all files.
     * @param fileEndingsToCompress Which file endings to compress
     * @return The new compression strategy
     */
    public static CompressionStrategy precompressAndValidate(int maxSizeBytes, String... fileEndingsToCompress) {
        return new CompressionStrategy(CompressionMode.PRECOMPRESS_VALIDATE, maxSizeBytes, fileEndingsToCompress);
    }
}
