package de.nocoffeetech.webservices.core.internal.loader;

import de.nocoffeetech.webservices.core.config.server.RealServerConfig;
import de.nocoffeetech.webservices.core.config.server.RootConfig;
import de.nocoffeetech.webservices.core.config.server.ServerConfig;
import de.nocoffeetech.webservices.core.config.server.VirtualServerConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.config.service.SingleInstanceServiceConfig;
import de.nocoffeetech.webservices.core.file.FileHolder;
import de.nocoffeetech.webservices.core.internal.WebserviceLookup;
import de.nocoffeetech.webservices.core.internal.config.Configuration;
import de.nocoffeetech.webservices.core.service.InvalidConfigValueException;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;
import de.nocoffeetech.webservices.core.service.holder.ServiceHolder;
import de.nocoffeetech.webservices.core.service.holder.ServiceHolderLookup;
import de.nocoffeetech.webservices.core.terminal.TerminalHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

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

        List<ServiceHolder<?>> services;
        try {
            services = createServices(rootConfig, config2Definition);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create service holders!", e);
        }
        ServiceHolderLookup.setServices(services);

        if (DEV_MODE) {
            Thread thread = new Thread(Loader::periodicDevTasks);
            thread.setPriority(3);
            thread.setName("Periodic Dev Tasks Runner");
            thread.setDaemon(true);
            thread.start();
        }

        TerminalHandler.setup();
    }

    private static void periodicDevTasks() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // huh
            }
            FileHolder.reloadAll();
        }
    }

    private static Map<BaseServiceConfig, WebserviceDefinition<?>> collectConfigs(RootConfig rootConfig, WebserviceLookup lookup) {
        Map<BaseServiceConfig, WebserviceDefinition<?>> allConfigs = new HashMap<>();
        Set<String> instanceNames = new HashSet<>();

        for (ServerConfig serverConfig : rootConfig.servers) {
            List<BaseServiceConfig> configsForServer = serverConfig.gatherServices();
            for (BaseServiceConfig baseServiceConfig : configsForServer) {
                try {
                    baseServiceConfig.validateConfig();
                } catch (InvalidConfigValueException e) {
                    throw new RuntimeException("Config validation of service " + baseServiceConfig.serviceIdentifier + " failed!", e);
                }
                WebserviceDefinition<?> definition = lookup.getFromSpec(baseServiceConfig.serviceIdentifier);
                if (baseServiceConfig instanceof SingleInstanceServiceConfig) {
                    if (allConfigs.containsValue(definition)) {
                        throw new RuntimeException("Single Instance service " + baseServiceConfig.serviceIdentifier + " has been configured multiple times!");
                    }
                }
                String instanceName = baseServiceConfig.getInstanceName();
                if (instanceName == null) {
                    throw new RuntimeException("Missing required parameter instanceName!");
                }
                if (!instanceNames.add(instanceName)) {
                    throw new RuntimeException("Instance name " + instanceName + " has been configured for two services!");
                }
                allConfigs.put(baseServiceConfig, definition);
            }
        }

        return allConfigs;
    }

    private static List<ServiceHolder<?>> createServices(RootConfig config, Map<BaseServiceConfig, WebserviceDefinition<?>> gatheredConfigs) throws IOException {
        List<ServiceHolder<?>> serviceHolders = new ArrayList<>();
        for (ServerConfig serverConfig : config.servers) {
            if (serverConfig instanceof RealServerConfig realServerConfig) {
                BaseServiceConfig serviceConfig = realServerConfig.service;
                WebserviceDefinition<?> definition = Objects.requireNonNull(gatheredConfigs.get(serviceConfig));
                serviceHolders.add(new ServiceHolder<>(definition, realServerConfig, serviceConfig));
            } else if (serverConfig instanceof VirtualServerConfig virtualServerConfig) {
                throw new RuntimeException("Virtual server config not yet implemented!");
            } else {
                throw new RuntimeException("Invalid configuration of type " + serverConfig.getClass());
            }
        }
        return serviceHolders;
    }
}
