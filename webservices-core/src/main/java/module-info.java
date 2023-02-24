import de.nocoffeetech.webservices.core.builtin.BuiltinServiceProvider;
import de.nocoffeetech.webservices.core.service.ServiceProvider;
import de.nocoffeetech.webservices.core.terminal.BuiltinCommandProvider;
import de.nocoffeetech.webservices.core.terminal.api.CommandProvider;

module de.nocoffeetech.webservices.core {
    exports de.nocoffeetech.webservices.core.builtin;
    exports de.nocoffeetech.webservices.core.builtin.config;
    exports de.nocoffeetech.webservices.core.config.service;
    exports de.nocoffeetech.webservices.core.endpoint;
    exports de.nocoffeetech.webservices.core.file;
    exports de.nocoffeetech.webservices.core.file.compress;
    exports de.nocoffeetech.webservices.core.service;
    exports de.nocoffeetech.webservices.core.service.holder;
    exports de.nocoffeetech.webservices.core.terminal.api;

    // for config deserialization
    exports de.nocoffeetech.webservices.core.config.server to com.google.gson;

    opens de.nocoffeetech.webservices.core.internal.loader;

    requires transitive de.nocoffeetech.smallhttp;
    requires transitive org.apache.logging.log4j;
    requires transitive com.google.gson;
    requires com.aayushatharva.brotli4j;

    // terminal stuff
    requires brigadier;
    requires org.jline.reader;
    requires net.minecrell.terminalconsole;
    requires org.jline.terminal;

    // GUI stuff
    requires static java.desktop;
    requires org.apache.logging.log4j.core;
    exports de.nocoffeetech.webservices.core.internal.gui to org.apache.logging.log4j.core;

    uses ServiceProvider;
    uses CommandProvider;
    provides ServiceProvider with BuiltinServiceProvider;
    provides CommandProvider with BuiltinCommandProvider;
}