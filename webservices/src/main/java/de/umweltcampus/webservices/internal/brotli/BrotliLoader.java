package de.umweltcampus.webservices.internal.brotli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BrotliLoader {
    private static final Logger LOGGER = LogManager.getLogger(BrotliLoader.class);
    private static final boolean BROTLI_PRESENT = hasBrotliLib();

    private static boolean hasBrotliLib() {
        try {
            Class.forName("com.aayushatharva.brotli4j.Brotli4jLoader");
            LOGGER.debug("Brotli lib present");
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.info("Brotli lib not present", e);
            return false;
        }
    }

    public static BrotliCompressor getBrotliCompressor() {
        if (BROTLI_PRESENT && BrotliCompressor.INSTANCE.canUse()) return BrotliCompressor.INSTANCE;
        else return null;
    }
}
