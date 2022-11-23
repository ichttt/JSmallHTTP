package de.umweltcampus.webservices.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ApplicationLocator {
    private static final String LIBRARY_PATH = System.getProperty("webservices.library_path", "./libs/");
    private static final String MODULES_PATH = System.getProperty("webservices.library_path", "./modules/");


    public static Path[] findPossibleLibraryPaths() throws IOException {
        return list(LIBRARY_PATH).toArray(Path[]::new);
    }

    public static Path[] findPossibleModulePaths() throws IOException {
        List<Path> fromDir = list(MODULES_PATH);
        List<Path> allPaths = new ArrayList<>(fromDir);
        // Add classpath modules
        allPaths.addAll(Arrays.stream(System.getProperty("java.class.path").split(":")).map(Paths::get).filter(Files::exists).toList());
        return allPaths.toArray(Path[]::new);
    }

    private static List<Path> list(String base) throws IOException {
        Path dirPath = Paths.get(base);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return Collections.emptyList();
        }
        try (Stream<Path> paths = Files.list(dirPath)) {
            return paths.filter(path -> path.toString().endsWith(".jar") || Files.isDirectory(path)).toList();
        }
    }
}
