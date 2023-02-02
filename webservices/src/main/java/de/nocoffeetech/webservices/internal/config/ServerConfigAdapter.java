package de.nocoffeetech.webservices.internal.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.nocoffeetech.webservices.config.server.RealServerConfig;
import de.nocoffeetech.webservices.config.server.ServerConfig;
import de.nocoffeetech.webservices.config.server.VirtualServerConfig;

import java.lang.reflect.Type;

public class ServerConfigAdapter implements JsonDeserializer<ServerConfig> {

    @Override
    public ServerConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        String type = root.get("type").getAsString();
        if (type.equals("virtual")) {
            return context.deserialize(root, VirtualServerConfig.class);
        } else if (type.equals("real")) {
            return context.deserialize(root, RealServerConfig.class);
        }
        return null;
    }
}
