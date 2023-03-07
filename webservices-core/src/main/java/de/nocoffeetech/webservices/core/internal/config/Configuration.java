package de.nocoffeetech.webservices.core.internal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.nocoffeetech.webservices.core.config.global.GlobalConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.config.server.RootConfig;
import de.nocoffeetech.webservices.core.config.server.ServerConfig;
import de.nocoffeetech.webservices.core.internal.service.loader.GlobalConfigServiceLoader;
import de.nocoffeetech.webservices.core.internal.service.loader.WebserviceServiceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Configuration {
    private final RootConfig rootConfig;

    public Configuration(WebserviceServiceLoader webserviceLoader, GlobalConfigServiceLoader globalConfigLoader) throws IOException {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(ServerConfig.class, new ServerConfigAdapter())
                .registerTypeAdapter(BaseServiceConfig.class, new BaseServiceConfigAdapter(webserviceLoader))
                .registerTypeAdapter(new TypeToken<Map<String, GlobalConfig>>() {}.getType(), new GlobalConfigMapAdapter(globalConfigLoader))
                .create();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get("config.json"))) {
            this.rootConfig = gson.fromJson(reader, RootConfig.class);
        }
    }

    public RootConfig getRootConfig() {
        return rootConfig;
    }
}
