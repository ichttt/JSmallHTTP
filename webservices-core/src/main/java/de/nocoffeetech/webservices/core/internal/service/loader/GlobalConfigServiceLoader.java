package de.nocoffeetech.webservices.core.internal.service.loader;

import de.nocoffeetech.webservices.core.config.global.GlobalConfigProvider;

import java.util.Map;

public class GlobalConfigServiceLoader extends BaseServiceLoader<GlobalConfigProvider<?>> {

    @SuppressWarnings("unchecked")
    private static Class<GlobalConfigProvider<?>> getGlobalConfigProviderClass() {
        // Evil fucking hack. It works. Don't touch it. Don't question its existence.
        // I already spend way too long searching for a better way. Just leave it alone unless you know a better way.
        return (Class<GlobalConfigProvider<?>>) (Object) GlobalConfigProvider.class;
    }

    public GlobalConfigServiceLoader() {
        super(getGlobalConfigProviderClass());
    }

    @Override
    protected String getName(GlobalConfigProvider<?> instance) {
        return instance.getConfigName();
    }

    public GlobalConfigProvider<?> getForName(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) throw new IllegalArgumentException("Invalid global config name " + name);

        GlobalConfigProvider<?> globalConfigProvider = this.serviceProviders.get(name);
        if (globalConfigProvider == null) {
            throw new IllegalArgumentException("Could not find global config provider for name " + name);
        }
        return globalConfigProvider;
    }
}
