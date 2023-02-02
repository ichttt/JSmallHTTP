package de.nocoffeetech.webservices.internal.brotli;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BrotliCompressor {
    private static final Logger LOGGER = LogManager.getLogger(BrotliCompressor.class);
    private static final Encoder.Parameters PARAMETERS = new Encoder.Parameters().setMode(Encoder.Mode.TEXT).setQuality(11);
    static final BrotliCompressor INSTANCE = new BrotliCompressor();

    private BrotliCompressor() {
        if (!Brotli4jLoader.isAvailable()) {
            Throwable unavailabilityCause = Brotli4jLoader.getUnavailabilityCause();
            LOGGER.warn("Failed to initialize brotli!", unavailabilityCause);
        } else {
            LOGGER.info("Brotli is ready to use");
        }
    }

    boolean canUse() {
        return Brotli4jLoader.isAvailable();
    }

    public OutputStream getCompressingOutputStream(Path path) throws IOException {
        return new BrotliOutputStream(Files.newOutputStream(path), PARAMETERS);
    }
}
