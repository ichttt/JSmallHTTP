import de.nocoffeetech.webservices.core.builtin.BuiltinServiceProvider;
import de.nocoffeetech.webservices.core.service.ServiceProvider;

module de.nocoffeetech.webservices.core {
    exports de.nocoffeetech.webservices.core.builtin;
    exports de.nocoffeetech.webservices.core.builtin.config;
    exports de.nocoffeetech.webservices.core.config.service;
    exports de.nocoffeetech.webservices.core.endpoint;
    exports de.nocoffeetech.webservices.core.file;
    exports de.nocoffeetech.webservices.core.file.compress;
    exports de.nocoffeetech.webservices.core.service;

    // for config deserialization
    exports de.nocoffeetech.webservices.core.config.server to com.google.gson;

    opens de.nocoffeetech.webservices.core.internal.loader;

    requires transitive de.nocoffeetech.smallhttp;
    requires transitive org.apache.logging.log4j;
    requires transitive com.google.gson;
    requires com.aayushatharva.brotli4j;

    uses ServiceProvider;
    provides ServiceProvider with BuiltinServiceProvider;
}