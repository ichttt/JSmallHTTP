package de.umweltcampus.webservices.internal;

import de.umweltcampus.smallhttp.HTTPServer;
import de.umweltcampus.smallhttp.HTTPServerBuilder;
import de.umweltcampus.webservices.config.BaseServiceConfig;
import de.umweltcampus.webservices.config.RealServerConfig;
import de.umweltcampus.webservices.config.ServerConfig;
import de.umweltcampus.webservices.config.VirtualServerConfig;
import de.umweltcampus.webservices.internal.config.Configuration;
import de.umweltcampus.webservices.service.InvalidConfigValueException;
import de.umweltcampus.webservices.service.WebserviceBase;
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
            LOGGER.fatal("Failed to start up!", e);
            System.exit(-1); // in case some threads are already running
        }
    }

    private static void launch() throws IOException {
        if (DEV_MODE) {
            System.setProperty("smallhttp.trackResponses", "true");
        }
        WebserviceLookup webservices = new WebserviceLookup();

        Configuration configuration;
        try {
            configuration = new Configuration(webservices);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read config!", e);
        }

        try {
            startupServers(configuration, webservices);
        } catch (InvalidConfigValueException e) {
            throw new RuntimeException("Invalid configuration!", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start up servers!", e);
        }
    }

    private static void startupServers(Configuration configuration, WebserviceLookup webservices) throws IOException, InvalidConfigValueException {
        for (ServerConfig serverConfig : configuration.getRootConfig().servers) {
            if (serverConfig instanceof RealServerConfig realServerConfig) {
                BaseServiceConfig service = realServerConfig.service;
                String serviceIdentifier = service.serviceIdentifier;
                service.validateConfig();

                WebserviceBase webservice = webservices.getFromSpec(serviceIdentifier).createNew(service, serviceIdentifier + "@" + realServerConfig.port);
                HTTPServer server = HTTPServerBuilder.create(realServerConfig.port, webservice).build();

                LOGGER.info("Started {}", webservice.getName());
            } else if (serverConfig instanceof VirtualServerConfig virtualServerConfig) {
                throw new RuntimeException("TODO");
            } else {
                throw new RuntimeException("Invalid configuration of type " + serverConfig.getClass());
            }
        }
    }
}
