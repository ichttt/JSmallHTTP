package de.umweltcampus.webservices.launcher;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.Collections;

public class Launcher {


    public static void main(String[] args) {
        System.out.println(System.getProperty("java.class.path"));
        ModuleLayer parent = ModuleLayer.boot();

        ModuleLayer pluginLayer;
        try {
            Path[] libraryModules = ApplicationLocator.findPossibleLibraryPaths();
            ClassLoader scl = ClassLoader.getSystemClassLoader();
            if (libraryModules != null && libraryModules.length > 0) {
                Configuration libraryConfig = parent.configuration().resolveAndBind(ModuleFinder.of(libraryModules), ModuleFinder.of(), Collections.emptySet());
                parent = parent.defineModulesWithOneLoader(libraryConfig, scl);
            }

            Path[] pluginModules = ApplicationLocator.findPossibleModulePaths();
            if (pluginModules == null || pluginModules.length == 0) {
                throw new RuntimeException("Failed to find any plugins!");
            }
            Configuration pluginConfig = parent.configuration().resolveAndBind(ModuleFinder.of(pluginModules), ModuleFinder.of(), Collections.singleton("de.umweltcampus.webservices"));
            pluginLayer = parent.defineModulesWithOneLoader(pluginConfig, scl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to boot module system!", e);
        }

        MethodHandle handle;
        try {
            Class<?> loaderClass = pluginLayer.findLoader("de.umweltcampus.webservices").loadClass("de.umweltcampus.webservices.internal.loader.Loader");
            handle = MethodHandles.lookup().findStatic(loaderClass, "init", MethodType.methodType(void.class));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find loader!", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access init method!", e);
        }

        try {
            handle.invoke();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1); // in case some threads are already running
        }

    }
}
