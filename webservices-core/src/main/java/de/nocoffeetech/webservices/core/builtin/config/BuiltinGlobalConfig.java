package de.nocoffeetech.webservices.core.builtin.config;

import de.nocoffeetech.webservices.core.config.global.GlobalConfig;

public class BuiltinGlobalConfig extends GlobalConfig {
    public final boolean enableGui;

    protected BuiltinGlobalConfig(boolean enableGui) {
        super();
        this.enableGui = enableGui;
    }
}
