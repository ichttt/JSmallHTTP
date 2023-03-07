package de.nocoffeetech.webservices.core.internal.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import de.nocoffeetech.webservices.core.config.global.GlobalConfig;
import de.nocoffeetech.webservices.core.config.global.GlobalConfigProvider;
import de.nocoffeetech.webservices.core.internal.service.loader.GlobalConfigServiceLoader;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class GlobalConfigMapAdapter implements JsonDeserializer<Map<String, GlobalConfig>> {
    private GlobalConfigServiceLoader globalConfigLoader;

    public GlobalConfigMapAdapter(GlobalConfigServiceLoader globalConfigLoader) {
        this.globalConfigLoader = globalConfigLoader;
    }

    @Override
    public Map<String, GlobalConfig> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<String, GlobalConfig> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().asMap().entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            GlobalConfigProvider<?> provider = this.globalConfigLoader.getForName(key);
            Class<?> configClass = provider.getConfigClass();
            GlobalConfig deserializedConfig = context.deserialize(value, configClass);
            map.put(key, deserializedConfig);
        }
        return map;
    }
}
