package de.umweltcampus.webservices.internal.loader;

import de.umweltcampus.smallhttp.base.HTTPServer;
import de.umweltcampus.smallhttp.base.HTTPServerBuilder;
import de.umweltcampus.webservices.config.BaseServiceConfig;
import de.umweltcampus.webservices.config.RealServerConfig;
import de.umweltcampus.webservices.config.ServerConfig;
import de.umweltcampus.webservices.config.VirtualServerConfig;
import de.umweltcampus.webservices.internal.WebserviceLookup;
import de.umweltcampus.webservices.internal.config.Configuration;
import de.umweltcampus.webservices.service.InvalidConfigValueException;
import de.umweltcampus.webservices.service.WebserviceBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Loader {
    private static final Logger LOGGER;
    private static final boolean DEV_MODE = Boolean.getBoolean("webservices.dev");

    static {
        long startTime = System.currentTimeMillis();
        LOGGER = LogManager.getLogger(Loader.class);
        long stopTime = System.currentTimeMillis();
        LOGGER.debug("Started up log4j in {} ms", (stopTime - startTime));
    }

    /**
     * Called from module launcher. DO NOT CHANGE SIGNATURE OR NAME! The loader holds a static reference to this
     */
    public static void init() {
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

                WebserviceBase webservice = webservices.getFromSpec(serviceIdentifier).createNew(service, serviceIdentifier + "-" + realServerConfig.port);
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
