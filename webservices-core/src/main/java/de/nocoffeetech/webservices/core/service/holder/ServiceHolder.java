package de.nocoffeetech.webservices.core.service.holder;

import de.nocoffeetech.smallhttp.base.HTTPServer;
import de.nocoffeetech.smallhttp.base.HTTPServerBuilder;
import de.nocoffeetech.webservices.core.config.server.RealServerConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
import de.nocoffeetech.webservices.core.internal.server.SmallHTTPErrorHandler;
import de.nocoffeetech.webservices.core.internal.service.RunningTask;
import de.nocoffeetech.webservices.core.service.ContinuousBackgroundTask;
import de.nocoffeetech.webservices.core.service.WebserviceBase;
import de.nocoffeetech.webservices.core.service.WebserviceDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO decouple from RealServerConfig once Virtual Servers become a thing
public class ServiceHolder<T extends BaseServiceConfig> {
    private static final Logger LOGGER = LogManager.getLogger(ServiceHolder.class);
    private static final int MAX_TERMINATION_TIME_MS = 10000;
    private final WebserviceDefinition<?> serviceDefinition;
    private final RealServerConfig realServerConfig;
    private final T serviceConfig;
    private final List<RunningTask> runningTasks = new ArrayList<>();
    private WebserviceBase runningService;
    private HTTPServer runningServer;
    private boolean inShutdownProcess = false;

    public ServiceHolder(WebserviceDefinition<T> serviceDefinition, RealServerConfig realServerConfig, BaseServiceConfig serviceConfig) throws IOException {
        this.serviceDefinition = serviceDefinition;
        this.realServerConfig = realServerConfig;
        this.serviceConfig = serviceDefinition.getConfigClass().cast(serviceConfig);
        if (serviceConfig.autostart)
            startup();
    }

    public void startup() throws IOException {
        synchronized (this) {
            if (isRunning()) throw new IllegalStateException("Already running!");

            String instanceName = serviceConfig.getInstanceName();
            LOGGER.info("Starting up {}", instanceName);

            WebserviceBase webservice = serviceDefinition.createNew(serviceConfig, instanceName);
            HTTPServer server = HTTPServerBuilder
                    .create(realServerConfig.port, webservice)
                    .setErrorHandler(new SmallHTTPErrorHandler(instanceName))
                    .build();
            this.runningService = webservice;
            this.runningServer = server;

            LOGGER.info("Started {}", runningService.getInstanceName());

            try {
                List<ContinuousBackgroundTask> backgroundTasks = serviceDefinition.getBackgroundTasks();
                for (ContinuousBackgroundTask backgroundTask : backgroundTasks) {
                    runningTasks.add(new RunningTask(webservice.getInstanceName(), backgroundTask, this::onBackgroundTaskDied));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to startup {}: Dependency tasks failed to launch!", webservice.getInstanceName(), e);
                shutdownIfPossible();
            }
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (!isRunning()) throw new IllegalStateException("Server already shutdown!");
            doShutdown();
        }
    }

    public void shutdownIfPossible() {
        synchronized (this) {
            if (runningService != null) {
                doShutdown();
            }
        }
    }

    private void doShutdown() {
        try {
            String instanceName = getInstanceName();
            LOGGER.info("Shutting down {}", instanceName);
            inShutdownProcess = true;

            try {
                runningServer.shutdown(true);
            } catch (IOException e) {
                LOGGER.error("Failed to shut down main server!");
                return;
            }

            Iterator<RunningTask> iterator = runningTasks.iterator();
            while (iterator.hasNext()) {
                RunningTask next = iterator.next();
                if (!next.isAlive())
                    iterator.remove();
                next.shutdown();
            }
            long stopTime = System.currentTimeMillis() + MAX_TERMINATION_TIME_MS;
            for (RunningTask runningTask : runningTasks) {
                try {
                    long currentTime = System.currentTimeMillis();
                    runningTask.awaitTermination(Math.max(10, stopTime - currentTime));
                } catch (RuntimeException e) {
                    LOGGER.error("Failed to stop task {} of service {}", runningTask.name(), instanceName, e);
                }
            }
            runningTasks.clear();
            runningService = null;
            runningServer = null;
        } finally {
            inShutdownProcess = false;
        }
    }

    public boolean isRunning() {
        synchronized (this) {
            return runningService != null && !runningServer.isShutdown();
        }
    }
    public WebserviceBase getServiceIfRunning() {
        synchronized (this) {
            if (isRunning()) return runningService;
            else return null;
        }
    }

    public WebserviceDefinition<?> getServiceDefinition() {
        return serviceDefinition;
    }

    public String getInstanceName() {
        return serviceConfig.getInstanceName();
    }

    private void onBackgroundTaskDied(String name) {
        if (inShutdownProcess) return;
        // Migrate to a new thread to avoid killing ourselves
        Thread thread = new Thread(() -> {
            LOGGER.warn("The background task {} died! Initiating shutdown of {}", name, serviceDefinition.getName());
            shutdownIfPossible();
        });
        thread.setName(serviceDefinition.getName() + " background crash shutdown");
        thread.setDaemon(true);
        thread.start();
    }
}
