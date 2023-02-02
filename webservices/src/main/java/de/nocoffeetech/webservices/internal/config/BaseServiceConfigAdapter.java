package de.nocoffeetech.webservices.internal.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.nocoffeetech.webservices.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.service.WebserviceDefinition;
import de.nocoffeetech.webservices.internal.WebserviceLookup;

import java.lang.reflect.Type;

public class BaseServiceConfigAdapter implements JsonDeserializer<BaseServiceConfig> {
    private final WebserviceLookup lookup;

    public BaseServiceConfigAdapter(WebserviceLookup lookup) {
        this.lookup = lookup;
    }


    @Override
    public BaseServiceConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        String serviceIdentifier = root.get("serviceIdentifier").getAsString();

        WebserviceDefinition<?> definition;
        try {
            definition = this.lookup.getFromSpec(serviceIdentifier);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find service for spec " + serviceIdentifier, e);
        }

        Class<? extends BaseServiceConfig> configClass = definition.getConfigClass();
        if (configClass == BaseServiceConfig.class) {
            // special case: parse by hand, as otherwise we recursively try to resolve the actual class
            return new BaseServiceConfig(serviceIdentifier);
        }
        return context.deserialize(root, configClass);
    }
}
