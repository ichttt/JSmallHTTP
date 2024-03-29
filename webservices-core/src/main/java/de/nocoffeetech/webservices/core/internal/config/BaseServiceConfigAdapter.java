package de.nocoffeetech.webservices.core.internal.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;
import de.nocoffeetech.webservices.core.internal.service.loader.WebserviceServiceLoader;

import java.lang.reflect.Type;

public class BaseServiceConfigAdapter implements JsonDeserializer<BaseServiceConfig> {
    private final WebserviceServiceLoader lookup;

    public BaseServiceConfigAdapter(WebserviceServiceLoader lookup) {
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

        return context.deserialize(root, definition.getConfigClass());
    }
}
