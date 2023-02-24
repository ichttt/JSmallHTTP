package de.nocoffeetech.webservices.core.internal.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class GuiLoader {
    private static final Logger LOGGER = LogManager.getLogger(GuiLoader.class);

    public static void loadIfApplicable() {
        if (isHeadless()) {
            LOGGER.info("Headless system, no starting gui");
            GuiLogAppender.deactivate();
            return;
        }
        load();
    }

    private static void load() {
        new TerminalGui();
    }

    private static boolean isHeadless() {
        try {
            Class<?> graphicsEnvClass = Class.forName("java.awt.GraphicsEnvironment");
            MethodHandle isHeadless = MethodHandles.lookup().findStatic(graphicsEnvClass, "isHeadless", MethodType.methodType(boolean.class));
            return (boolean) isHeadless.invoke();
        } catch (Throwable e) {
            LOGGER.warn("Failed to determine headless state", e);
            return false;
        }
    }
}
