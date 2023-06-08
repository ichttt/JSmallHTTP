package de.nocoffeetech.webservices.core.internal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.nocoffeetech.webservices.core.config.global.GlobalConfig;
import de.nocoffeetech.webservices.core.config.server.RootConfig;
import de.nocoffeetech.webservices.core.config.server.ServerConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.internal.service.loader.GlobalConfigServiceLoader;
import de.nocoffeetech.webservices.core.internal.service.loader.WebserviceServiceLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Configuration {
    private static final Logger LOGGER = LogManager.getLogger(Configuration.class);
    private final RootConfig rootConfig;

    public Configuration(WebserviceServiceLoader webserviceLoader, GlobalConfigServiceLoader globalConfigLoader) throws IOException {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(ServerConfig.class, new ServerConfigAdapter())
                .registerTypeAdapter(BaseServiceConfig.class, new BaseServiceConfigAdapter(webserviceLoader))
                .registerTypeAdapter(new TypeToken<Map<String, GlobalConfig>>() {}.getType(), new GlobalConfigMapAdapter(globalConfigLoader))
                .create();
        Path configPath = Paths.get("config.json");
        LOGGER.info("Parsing config file from {}", configPath.toAbsolutePath().toString());
        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            this.rootConfig = gson.fromJson(reader, RootConfig.class);
        }
    }

    public RootConfig getRootConfig() {
        return rootConfig;
    }
}
