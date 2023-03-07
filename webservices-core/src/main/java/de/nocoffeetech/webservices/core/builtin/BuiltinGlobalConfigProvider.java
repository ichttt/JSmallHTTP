package de.nocoffeetech.webservices.core.builtin;

import de.nocoffeetech.webservices.core.builtin.config.BuiltinGlobalConfig;
import de.nocoffeetech.webservices.core.config.global.GlobalConfigProvider;

public class BuiltinGlobalConfigProvider implements GlobalConfigProvider<BuiltinGlobalConfig> {

    @Override
    public Class<BuiltinGlobalConfig> getConfigClass() {
        return BuiltinGlobalConfig.class;
    }

    @Override
    public String getConfigName() {
        return "builtin";
    }
}
