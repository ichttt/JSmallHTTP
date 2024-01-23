# JSmallHTTP / Webservices

Welcome to the JSmallHTTP / Webservices repository! Here, you'll find a compact HTTP/1.1 Web Server along with a Web Service Framework built on top of it.

## JSmallHTTP

JSmallHTTP is a lightweight, standalone, and versatile HTTP/1.1 server library.
Crafted in Java 17 without external dependencies, it places a strong emphasis on achieving high throughput.

### Key Features:

- **Lightweight:** Small in size with minimal dependencies.
- **Standalone:** Does not rely on external libraries.
- **Flexibility:** Designed for flexibility to suit various use cases.
- **High Throughput:** Optimized for efficient data transfer.

## Webservices

Nocoffeetech Webservices is a framework built on the foundation of JSmallHTTP.
It extends support for modular web services and introduces additional features such as a static file server.

### Key Features:

- **Modularity:** Supports modular web services for easy extensibility.
- **Config Support:** Configurable via `config.json` for easy customization.
- **Static File Server:** Allows serving static files from configured directories.
- **Standalone Application:** Can be initiated using a dedicated launcher.
- **JPMS Integration:** Utilizes the Java Platform Module System for better modularization.

## Getting Started

To begin exploring Webservices, follow these steps:

1. Run `./gradlew build` to build the project.
2. Start the example server located in the `build/release` folder using the command `java -jar webservices-launcher.jar`.

## Setting up a module development workspace

If you want to develop a module or plugin that can be loaded by WebServices, you need to set up a workspace for that.
The following gradle buildscript provides a basis for that:

```groovy
import java.util.stream.Collectors

plugins {
    id 'java'
}

group 'de.nocoffeetech'
version '1.0-SNAPSHOT'

repositories {
    mavenLocal()
    mavenCentral()
}

configurations {
    launcherTool
}

dependencies {
    implementation group: 'de.nocoffeetech', name: 'webservices-core', version: '1.0.2'
    launcherTool group: 'de.nocoffeetech', name: 'webservices-launcher', version: '1.0.2'
}

project.file("run").mkdir()
task run(type: JavaExec) {
    classpath project.configurations.launcherTool
    workingDir = "run"
    jvmArgs = ["-Dwebservices.dev=true", "-Dwebservices.launcher.additional_modules=" + sourceSets.main.runtimeClasspath.toList().stream().map(Object::toString).collect(Collectors.joining(File.pathSeparator))]
}
run.dependsOn(classes)
```

For this to work, you have to export this project to your local maven using `./gradlew publishToMavenLocal`.
After that, use the buildscript provided to develop your plugin.