import de.nocoffeetech.webservices.builtin.BuiltinServiceProvider;
import de.nocoffeetech.webservices.service.ServiceProvider;

module de.nocoffeetech.webservices {
    exports de.nocoffeetech.webservices.builtin;
    exports de.nocoffeetech.webservices.builtin.config;
    exports de.nocoffeetech.webservices.config.service;
    exports de.nocoffeetech.webservices.endpoint;
    exports de.nocoffeetech.webservices.file;
    exports de.nocoffeetech.webservices.file.compress;
    exports de.nocoffeetech.webservices.service;

    // for config deserialization
    exports de.nocoffeetech.webservices.config.server to com.google.gson;

    opens de.nocoffeetech.webservices.internal.loader;

    requires transitive de.nocoffeetech.smallhttp;
    requires transitive org.apache.logging.log4j;
    requires transitive com.google.gson;
    requires com.aayushatharva.brotli4j;

    uses ServiceProvider;
    provides ServiceProvider with BuiltinServiceProvider;
}