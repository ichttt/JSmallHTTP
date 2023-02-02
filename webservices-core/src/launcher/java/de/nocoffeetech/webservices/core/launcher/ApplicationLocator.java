package de.nocoffeetech.webservices.core.launcher;

import java.io.File;
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
    private static final String LIBRARY_PATH = System.getProperty("webservices.launcher.library_path", "./libs/");
    private static final String MODULES_PATH = System.getProperty("webservices.launcher.plugin_path", "./plugins/");
    private static final String ADDITIONAL_MODULES = System.getProperty("webservices.launcher.additional_modules");


    public static List<Path> findPossibleLibraryPaths() throws IOException {
        return list(LIBRARY_PATH);
    }

    public static List<Path> findPossibleModulePaths() throws IOException {
        List<Path> fromDir = list(MODULES_PATH);
        List<Path> allPaths = new ArrayList<>(fromDir);
        // Add classpath modules
        String[] splitAdditionalModules = new String[0];
        if (ADDITIONAL_MODULES != null) {
            splitAdditionalModules = ADDITIONAL_MODULES.split(File.pathSeparator);
        }
        List<Path> classPathsToAdd = Arrays.stream(splitAdditionalModules)
                .map(Paths::get)
                .filter(Files::exists)
                .toList();
        allPaths.addAll(classPathsToAdd);
        return allPaths;
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
