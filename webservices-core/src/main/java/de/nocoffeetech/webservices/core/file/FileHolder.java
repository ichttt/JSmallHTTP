package de.nocoffeetech.webservices.core.file;

import de.nocoffeetech.smallhttp.data.Status;
import de.nocoffeetech.smallhttp.header.CommonContentTypes;
import de.nocoffeetech.smallhttp.response.FixedResponseBodyWriter;
import de.nocoffeetech.smallhttp.response.HTTPWriteException;
import de.nocoffeetech.smallhttp.response.ResponseStartWriter;
import de.nocoffeetech.smallhttp.response.ResponseToken;
import de.nocoffeetech.webservices.core.internal.util.JPMSUtil;
import de.nocoffeetech.webservices.core.service.WebserviceBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class FileHolder {
    private static final Logger LOGGER = LogManager.getLogger(FileHolder.class);
    private static final Set<FileHolder> ALL_HOLDER = Collections.newSetFromMap(new WeakHashMap<>());
    private final String path;
    private final WebserviceBase webserviceBase;
    private final String[] patterns;
    private Object[] parts;
    private int baseLength;
    private int[] multiplierArray;

    public static void reloadAll() {
        synchronized (FileHolder.class) {
            for (FileHolder fileHolder : ALL_HOLDER) {
                try {
                    fileHolder.reload();
                } catch (IOException e) {
                    LOGGER.warn("Failed to reload holder for service {} and file {}", fileHolder.webserviceBase.getInstanceName(), fileHolder.path);
                }
            }
        }
    }

    private static String[] generatePatterns(int patternLength) {
        String[] patterns = new String[patternLength];
        for (int i = 0; i < patternLength; i++) {
            patterns[i] = "{" + i + "}";
        }
        return patterns;
    }

    public FileHolder(String fileBase, WebserviceBase webserviceBase, int patternLength) {
        this(fileBase, webserviceBase, generatePatterns(patternLength));
    }

    public FileHolder(String fileBase, WebserviceBase webserviceBase, String[] patterns) {
        if (!fileBase.startsWith("/")) throw new IllegalArgumentException("File must be an absolute path in the jar!");
        this.path = fileBase;
        this.webserviceBase = webserviceBase;
        this.patterns = patterns;
        try {
            reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        synchronized (FileHolder.class) {
            ALL_HOLDER.add(this);
        }
        LOGGER.debug("New holder registered for service {} and file {}", webserviceBase.getInstanceName(), fileBase);
    }

    /**
     * Loads the file from the jar and parses it into its pattern.
     * The pattern consists of placeholders and literal string. This has the benefit that no string replace needs to occur at runtime,
     * but instead the output be piped to the client directly (see {@link #respond(ResponseStartWriter, String...)}
     */
    public void reload() throws IOException {
        String baseSite;
        Class<? extends WebserviceBase> webserviceClass = webserviceBase.getClass();
        JPMSUtil.SourceType sourceType = JPMSUtil.getSourceTypeForClass(webserviceClass);

        switch (sourceType) {
            case JAR -> {
                try (FileSystem fileSystem = JPMSUtil.openFileSystemFor(webserviceClass)) {
                    Path pathInJar = fileSystem.getPath(path.substring(1));
                    baseSite = Files.readString(pathInJar, StandardCharsets.UTF_8);
                }
            }
            case EXPLODED_DIR -> {
                baseSite = Files.readString(JPMSUtil.getPathInExplodedDirectorySource(path, webserviceClass), StandardCharsets.UTF_8);
            }
            default -> throw new RuntimeException("Unknown type " + sourceType);
        }


        int startIndex = 0;
        List<Object> partsNew = new ArrayList<>();
        int baseLengthNew = 0;
        int foundIndex;
        int patternIndex;
        int[] multiplierArrayNew = new int[patterns.length];
        do {
            foundIndex = Integer.MAX_VALUE;
            patternIndex = -1;
            for (int i = 0; i < patterns.length; i++) {
                String pattern = patterns[i];
                int newFoundIndex = baseSite.indexOf(pattern, startIndex);
                if (newFoundIndex != -1 && newFoundIndex < foundIndex) {
                    foundIndex = newFoundIndex;
                    patternIndex = i;
                }
            }
            if (foundIndex != Integer.MAX_VALUE) {
                String partString = baseSite.substring(startIndex, foundIndex);
                if (!partString.isEmpty()) {
                    partsNew.add(partString.getBytes(StandardCharsets.UTF_8));
                }
                partsNew.add(patternIndex);
                startIndex = foundIndex + patterns[patternIndex].length();
                baseLengthNew += partString.length();
                multiplierArrayNew[patternIndex] = multiplierArrayNew[patternIndex] + 1;
            }
        } while (foundIndex != Integer.MAX_VALUE);
        String restPartNew = baseSite.substring(startIndex);
        partsNew.add(restPartNew.getBytes(StandardCharsets.UTF_8));
        baseLengthNew += restPartNew.length();
        Object[] partsNewArr = partsNew.toArray();
        this.parts = partsNewArr;
        this.baseLength = baseLengthNew;
        this.multiplierArray = multiplierArrayNew;
    }

    protected ResponseToken respond(ResponseStartWriter startWriter, String... params) throws HTTPWriteException {
        Object[] parts = this.parts;
        int baseLength = this.baseLength;
        int[] multiplierArray = this.multiplierArray;
        if (params.length != multiplierArray.length)
            throw new IllegalArgumentException("Invalid length of args, expected " + multiplierArray.length + ", got " + params.length);

        int length = baseLength;
        for (int i = 0; i < params.length; i++) {
            length += (params[i].length() * multiplierArray[i]);
        }
        FixedResponseBodyWriter fixedResponseBodyWriter = startWriter.respond(Status.OK, CommonContentTypes.HTML).beginBodyWithKnownSize(length);
        try {
            OutputStream stream = fixedResponseBodyWriter.getRawOutputStream();
            for (Object part : parts) {
                if (part instanceof byte[])
                    stream.write((byte[]) part);
                else if (part instanceof Integer) {
                    stream.write(params[(int) part].getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new RuntimeException("Unexpected entry " + part + " of type " + part.getClass());
                }
            }
            return fixedResponseBodyWriter.finalizeResponse();
        } catch (IOException e) {
            throw new HTTPWriteException(e);
        }
    }
}
