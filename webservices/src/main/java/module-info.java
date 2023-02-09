import de.umweltcampus.webservices.builtin.BuiltinServiceProvider;
import de.umweltcampus.webservices.service.ServiceProvider;

module de.umweltcampus.webservices {
    exports de.umweltcampus.webservices.builtin;
    exports de.umweltcampus.webservices.builtin.config;
    exports de.umweltcampus.webservices.config.service;
    exports de.umweltcampus.webservices.endpoint;
    exports de.umweltcampus.webservices.file;
    exports de.umweltcampus.webservices.file.compress;
    exports de.umweltcampus.webservices.service;

    // for config deserialization
    exports de.umweltcampus.webservices.config.server to com.google.gson;

    opens de.umweltcampus.webservices.internal.loader;

    requires transitive de.umweltcampus.smallhttp;
    requires transitive org.apache.logging.log4j;
    requires transitive com.google.gson;
    requires com.aayushatharva.brotli4j;

    uses ServiceProvider;
    provides ServiceProvider with BuiltinServiceProvider;
}