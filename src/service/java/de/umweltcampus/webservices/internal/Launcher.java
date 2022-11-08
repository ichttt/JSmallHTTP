package de.umweltcampus.webservices.internal;

import de.umweltcampus.webservices.ServiceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class Launcher {
    private static final Logger LOGGER;
    private static final boolean DEV_MODE = Boolean.getBoolean("webservices.dev");

    static {
        long startTime = System.currentTimeMillis();
        LOGGER = LogManager.getLogger(Launcher.class);
        long stopTime = System.currentTimeMillis();
        LOGGER.debug("Started up log4j in {} ms", (stopTime - startTime));
    }

    public static void main(String[] args) {
        if (DEV_MODE) {
            LOGGER.warn("DEV MODE ENABLED");
        } else {
            LOGGER.debug("Starting in prod mode");
        }
        Thread.UncaughtExceptionHandler handler = (t, e) -> {
            LOGGER.fatal("Uncaught exception in thread {}", t.getName(), e);
            e.printStackTrace(); // also print the stacktrace, so we even get it if we are in shutdown and log4j already said goodbye
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);

        ServiceLoader<ServiceProvider> loader = ServiceLoader.load(ServiceProvider.class, Launcher.class.getClassLoader());

        List<ServiceProvider> serviceProviders = new ArrayList<>();
        for (ServiceProvider serviceProvider : loader) {
            serviceProviders.add(serviceProvider);
        }

        LOGGER.info("Found {} services", serviceProviders.size());

        for (ServiceProvider provider : serviceProviders) {
            Set<ServiceProvider> otherProviders = new HashSet<>(serviceProviders);
            otherProviders.remove(provider);
            provider.initialize(otherProviders);
        }
    }
}
