package de.nocoffeetech.webservices.core.internal.loader;

import de.nocoffeetech.webservices.core.builtin.BuiltinGlobalConfigProvider;
import de.nocoffeetech.webservices.core.builtin.config.BuiltinGlobalConfig;
import de.nocoffeetech.webservices.core.config.global.GlobalConfig;
import de.nocoffeetech.webservices.core.config.global.GlobalConfigProvider;
import de.nocoffeetech.webservices.core.config.server.RealServerConfig;
import de.nocoffeetech.webservices.core.config.server.RootConfig;
import de.nocoffeetech.webservices.core.config.server.ServerConfig;
import de.nocoffeetech.webservices.core.config.server.VirtualServerConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.config.service.SingleInstanceServiceConfig;
import de.nocoffeetech.webservices.core.file.FileHolder;
import de.nocoffeetech.webservices.core.internal.config.Configuration;
import de.nocoffeetech.webservices.core.internal.gui.GuiLoader;
import de.nocoffeetech.webservices.core.internal.service.loader.GlobalConfigServiceLoader;
import de.nocoffeetech.webservices.core.internal.service.loader.WebserviceServiceLoader;
import de.nocoffeetech.webservices.core.service.InvalidConfigValueException;
import de.nocoffeetech.webservices.core.service.VirtualServerManager;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;
import de.nocoffeetech.webservices.core.service.holder.*;
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

        WebserviceServiceLoader webservices = new WebserviceServiceLoader();
        GlobalConfigServiceLoader globalConfigs = new GlobalConfigServiceLoader();
        Configuration configuration = loadConfig(webservices, globalConfigs);

        setGlobalConfigs(globalConfigs, configuration);

        BuiltinGlobalConfig builtinGlobalConfig = GlobalConfigLookup.getByConfigClass(BuiltinGlobalConfigProvider.class);
        if (!builtinGlobalConfig.disableGui) {
            GuiLoader.loadIfApplicable();
        } else {
            LOGGER.info("GUI is disabled in config");
        }

        initAndStartupServices(webservices, configuration);

        if (DEV_MODE) {
            Thread thread = new Thread(Loader::periodicDevTasks);
            thread.setPriority(3);
            thread.setName("Periodic Dev Tasks Runner");
            thread.setDaemon(true);
            thread.start();
        }

        TerminalHandler.setup();
    }

    private static Configuration loadConfig(WebserviceServiceLoader webservices, GlobalConfigServiceLoader globalConfigs) {
        Configuration configuration;
        try {
            configuration = new Configuration(webservices, globalConfigs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read config!", e);
        }

        RootConfig rootConfig = configuration.getRootConfig();
        if (rootConfig.servers == null) {
            throw new RuntimeException("Missing \"servers\" section in config!");
        }
        if (rootConfig.globalConfigs == null) {
            throw new RuntimeException("Missing \"globalConfigs\" section in config!");
        }

        return configuration;
    }

    private static void setGlobalConfigs(GlobalConfigServiceLoader globalConfigs, Configuration baseConfig) {
        Map<String, GlobalConfigProvider<?>> globalConfigsByName = globalConfigs.getAll();
        Map<GlobalConfigProvider<?>, GlobalConfig> configProviderMap = new HashMap<>();
        for (Map.Entry<String, GlobalConfig> entry : baseConfig.getRootConfig().globalConfigs.entrySet()) {
            String key = entry.getKey();
            GlobalConfig config = entry.getValue();
            try {
                config.validateConfig();
            } catch (InvalidConfigValueException e) {
                throw new RuntimeException("Config validation of global config " + key + " failed!", e);
            }
            GlobalConfigProvider<?> provider = globalConfigsByName.remove(key);
            if (provider == null) throw new IllegalArgumentException("Could not find global config provider for name " + key);

            GlobalConfigServiceLoader.setConfig(provider, config);
            configProviderMap.put(provider, config);
        }
        // Check if any config has not been removed from the map. If that is the case then the config is missing entries
        if (!globalConfigsByName.isEmpty()) {
            LOGGER.error("{} global config entries could not be found!", globalConfigsByName.size());
            for (String name : globalConfigsByName.keySet()) {
                LOGGER.error("\tNo entry for global config \"{}\"", name);
            }
            throw new RuntimeException("Found " + globalConfigsByName.size() + " unresolved global configs");
        }
        GlobalConfigLookup.setCurrentConfigs(configProviderMap);
    }

    private static void initAndStartupServices(WebserviceServiceLoader webservices, Configuration configuration) {
        RootConfig rootConfig = configuration.getRootConfig();
        Map<BaseServiceConfig, WebserviceDefinition<?>> config2Definition = collectConfigs(rootConfig, webservices);

        webservices.initServices(config2Definition.values());
        LOGGER.debug("Services initialized");

        List<ServiceHolder<?>> services;
        try {
            services = createServices(rootConfig, config2Definition);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create service holders!", e);
        }
        ServiceHolderLookup.setServices(services);

        for (ServiceHolder<?> service : services) {
            if (service.getServiceConfiguration().autostart) {
                try {
                    service.startup();
                } catch (IOException e) {
                    LOGGER.error("Failed to start up server {}, skipping!", service.getInstanceName(), e);
                }
            }
        }
    }

    private static void periodicDevTasks() {
        while (true) {
            try {
                // TODO maybe, in the far far future, implement a file watcher so we dont have to poll. Not important though as dev only
                //noinspection BusyWait
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                // huh
            }
            FileHolder.reloadAll();
        }
    }

    private static Map<BaseServiceConfig, WebserviceDefinition<?>> collectConfigs(RootConfig rootConfig, WebserviceServiceLoader lookup) {
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

    private static List<ServiceHolder<?>> createServices(RootConfig config, Map<BaseServiceConfig, WebserviceDefinition<?>> gatheredConfigs) {
        List<ServiceHolder<?>> serviceHolders = new ArrayList<>();
        for (ServerConfig serverConfig : config.servers) {
            if (serverConfig instanceof RealServerConfig realServerConfig) {
                BaseServiceConfig serviceConfig = realServerConfig.service;
                WebserviceDefinition<?> definition = Objects.requireNonNull(gatheredConfigs.get(serviceConfig));
                serviceHolders.add(new RealServiceHolder<>(definition, realServerConfig, serviceConfig));
            } else if (serverConfig instanceof VirtualServerConfig virtualServerConfig) {
                List<BaseServiceConfig> serviceConfigs = virtualServerConfig.gatherServices();
                VirtualServerManager virtualServerManager = new VirtualServerManager(serverConfig.port);
                for (BaseServiceConfig serviceConfig : serviceConfigs) {
                    WebserviceDefinition<?> definition = Objects.requireNonNull(gatheredConfigs.get(serviceConfig));
                    String mapping = virtualServerConfig.mappings
                            .entrySet()
                            .stream()
                            .filter(virtualMapping -> virtualMapping.getValue() == serviceConfig)
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Failed to find mapping for service " + serviceConfig.getInstanceName()));
                    if (!mapping.startsWith("/") || !mapping.endsWith("/")) {
                        throw new RuntimeException("Virtual mapping for service " + serviceConfig.getInstanceName() + " needs to start with and end with a Slash ('/'), but was \"" + mapping + "\"!");
                    }
                    serviceHolders.add(new VirtualServiceHolder<>(definition, virtualServerConfig, serviceConfig, mapping, virtualServerManager));
                }
            } else {
                throw new RuntimeException("Invalid configuration of type " + serverConfig.getClass());
            }
        }
        return serviceHolders;
    }
}
