package de.umweltcampus.webservices.launcher;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Launcher {


    public static void main(String[] args) {
        System.out.println(System.getProperty("java.class.path"));
        ModuleLayer parent = ModuleLayer.boot();

        ModuleLayer pluginLayer;
        try {
            List<Path> libraryModules = new ArrayList<>(ApplicationLocator.findPossibleLibraryPaths());

            List<Path> pluginModules = ApplicationLocator.findPossibleModulePaths();
            if (pluginModules.isEmpty()) {
                throw new RuntimeException("Failed to find any plugins!");
            }
            libraryModules.addAll(pluginModules);
            Configuration pluginConfig = parent.configuration().resolveAndBind(ModuleFinder.of(libraryModules.toArray(Path[]::new)), ModuleFinder.of(), Collections.singleton("de.umweltcampus.webservices"));
            pluginLayer = parent.defineModulesWithOneLoader(pluginConfig, ClassLoader.getSystemClassLoader());
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
