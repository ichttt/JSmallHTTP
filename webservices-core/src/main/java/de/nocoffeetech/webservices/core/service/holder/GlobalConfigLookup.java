package de.nocoffeetech.webservices.core.service.holder;

import de.nocoffeetech.webservices.core.config.global.GlobalConfig;
import de.nocoffeetech.webservices.core.config.global.GlobalConfigProvider;

import java.util.Collections;
import java.util.Map;

public class GlobalConfigLookup {
    private static Map<GlobalConfigProvider<?>, GlobalConfig> currentConfigs;

    public static void setCurrentConfigs(Map<GlobalConfigProvider<?>, GlobalConfig> currentConfigs) {
        if (GlobalConfigLookup.currentConfigs != null) throw new IllegalStateException();
        GlobalConfigLookup.currentConfigs = Collections.unmodifiableMap(currentConfigs);
    }

    public static <T extends GlobalConfig> T getByConfigClass(Class<? extends GlobalConfigProvider<T>> providerClass) {
        for (Map.Entry<GlobalConfigProvider<?>, GlobalConfig> entry : currentConfigs.entrySet()) {
            GlobalConfigProvider<?> provider = entry.getKey();
            GlobalConfig globalConfig = entry.getValue();
            if (provider.getClass() == providerClass) {
                GlobalConfigProvider<T> castedProvider = (providerClass.cast(provider));
                return castedProvider.getConfigClass().cast(globalConfig);
            }
        }
        throw new IllegalArgumentException("Config provider class + " + providerClass.getName() + " is not provided to service loader!");

    }

    public static GlobalConfig getByConfigName(String name) {
        for (Map.Entry<GlobalConfigProvider<?>, GlobalConfig> entry : currentConfigs.entrySet()) {
            GlobalConfigProvider<?> configProvider = entry.getKey();
            if (configProvider.getConfigName().equals(name)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("Config for name " + name + " not found!");
    }

    public static Map<GlobalConfigProvider<?>, GlobalConfig> getAll() {
        return currentConfigs;
    }
}
