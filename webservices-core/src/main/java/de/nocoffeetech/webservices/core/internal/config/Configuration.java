package de.nocoffeetech.webservices.core.internal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.config.server.RootConfig;
import de.nocoffeetech.webservices.core.config.server.ServerConfig;
import de.nocoffeetech.webservices.core.internal.WebserviceLookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Configuration {
    private final RootConfig rootConfig;

    public Configuration(WebserviceLookup lookup) throws IOException {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .registerTypeAdapter(ServerConfig.class, new ServerConfigAdapter())
                .registerTypeAdapter(BaseServiceConfig.class, new BaseServiceConfigAdapter(lookup))
                .create();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get("config.json"))) {
            this.rootConfig = gson.fromJson(reader, RootConfig.class);
        }
    }

    public RootConfig getRootConfig() {
        return rootConfig;
    }
}
