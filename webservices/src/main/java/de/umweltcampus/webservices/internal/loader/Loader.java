package de.umweltcampus.webservices.internal.loader;

import de.umweltcampus.smallhttp.base.HTTPServer;
import de.umweltcampus.smallhttp.base.HTTPServerBuilder;
import de.umweltcampus.webservices.config.server.RealServerConfig;
import de.umweltcampus.webservices.config.server.VirtualServerConfig;
import de.umweltcampus.webservices.config.service.BaseServiceConfig;
import de.umweltcampus.webservices.config.server.RootConfig;
import de.umweltcampus.webservices.config.server.ServerConfig;
import de.umweltcampus.webservices.internal.WebserviceLookup;
import de.umweltcampus.webservices.internal.config.Configuration;
import de.umweltcampus.webservices.service.InvalidConfigValueException;
import de.umweltcampus.webservices.service.WebserviceBase;
import de.umweltcampus.webservices.service.WebserviceDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Loader {
    public static final boolean DEV_MODE = Boolean.getBoolean("webservices.dev");
    private static final Logger LOGGER;

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

        RootConfig rootConfig = configuration.getRootConfig();
        Map<BaseServiceConfig, WebserviceDefinition<?>> config2Definition = collectConfigs(rootConfig, webservices);

        webservices.initServices(config2Definition.values());

        try {
            startupServer(rootConfig, config2Definition);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start up servers!", e);
        }
    }

    private static Map<BaseServiceConfig, WebserviceDefinition<?>> collectConfigs(RootConfig rootConfig, WebserviceLookup lookup) {
        Map<BaseServiceConfig, WebserviceDefinition<?>> allConfigs = new HashMap<>();

        for (ServerConfig serverConfig : rootConfig.servers) {
            List<BaseServiceConfig> configsForServer = serverConfig.gatherServices();
            for (BaseServiceConfig baseServiceConfig : configsForServer) {
                try {
                    baseServiceConfig.validateConfig();
                } catch (InvalidConfigValueException e) {;
                    throw new RuntimeException("Config validation of service " + baseServiceConfig.serviceIdentifier + " failed!", e);
                }
                WebserviceDefinition<?> definition = lookup.getFromSpec(baseServiceConfig.serviceIdentifier);
                if (definition.isSingleInstanceOnly()) {
                    if (allConfigs.containsValue(definition)) {
                        throw new RuntimeException("Single Instance service " + baseServiceConfig.serviceIdentifier + " has been configured multiple times!");
                    }
                }
                allConfigs.put(baseServiceConfig, definition);
            }
        }

        return allConfigs;
    }

    private static void startupServer(RootConfig config, Map<BaseServiceConfig, WebserviceDefinition<?>> gatheredConfigs) throws IOException {
        for (ServerConfig serverConfig : config.servers) {
            if (serverConfig instanceof RealServerConfig realServerConfig) {
                BaseServiceConfig service = realServerConfig.service;
                WebserviceDefinition<?> definition = Objects.requireNonNull(gatheredConfigs.get(service));

                WebserviceBase webservice = definition.createNew(service, service.serviceIdentifier + "-" + realServerConfig.port);
                HTTPServer server = HTTPServerBuilder.create(realServerConfig.port, webservice).build();
                LOGGER.info("Started {}", webservice.getName());
            } else if (serverConfig instanceof VirtualServerConfig virtualServerConfig) {
                throw new RuntimeException("Virtual server config not yet implemented!");
            } else {
                throw new RuntimeException("Invalid configuration of type " + serverConfig.getClass());
            }
        }
    }
}
