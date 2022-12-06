package de.umweltcampus.webservices.internal.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class TempDirHelper {
    private static final String TMP_ROOT = Objects.requireNonNull(System.getProperty("java.io.tmpdir"));


    public static Path createTempPathFor(String webserviceName, String tempPrefix) {
        Path tmpBase = Paths.get(TMP_ROOT, "webservices", webserviceName.replace(':', '-'));
        Path compressedFilesFolder;
        try {
            Files.createDirectories(tmpBase);
            compressedFilesFolder = Files.createTempDirectory(tmpBase, "tmp_" + tempPrefix.replace('/', '_'));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create tmp dir!", e);
        }
        return compressedFilesFolder;
    }
}
