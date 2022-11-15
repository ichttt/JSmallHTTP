package de.umweltcampus.webservices.internal;

import de.umweltcampus.smallhttp.HTTPServer;
import de.umweltcampus.smallhttp.HTTPServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Launcher {
    private static final Logger LOGGER;
    private static final boolean DEV_MODE = Boolean.getBoolean("webservices.dev");

    static {
        long startTime = System.currentTimeMillis();
        LOGGER = LogManager.getLogger(Launcher.class);
        long stopTime = System.currentTimeMillis();
        LOGGER.debug("Started up log4j in {} ms", (stopTime - startTime));
    }

    public static void main(String[] args) {
        if (DEV_MODE) {
            LOGGER.warn("DEV MODE ENABLED");
        } else {
            LOGGER.debug("Starting in prod mode");
        }
        Thread.UncaughtExceptionHandler handler = (t, e) -> {
            LOGGER.fatal("Uncaught exception in thread {}", t.getName(), e);
            e.printStackTrace(); // also print the stacktrace, so we even get it if we are in shutdown and log4j already said goodbye
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
        try {
            launch();
        } catch (Exception e) {
            LOGGER.error("Failed to start up!", e);
            System.exit(-1); // in case some threads are already running
        }
    }

    private static void launch() throws IOException {
        WebserviceManager webservices = new WebserviceManager();

        HTTPServer server = HTTPServerBuilder.create(8080, webservices.createFromSpec("builtin:simple_fileserver")).build();
    }
}
