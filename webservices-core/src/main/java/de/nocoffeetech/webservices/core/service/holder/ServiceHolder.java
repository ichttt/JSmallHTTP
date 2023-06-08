package de.nocoffeetech.webservices.core.service.holder;

import de.nocoffeetech.webservices.core.config.server.ServerConfig;
import de.nocoffeetech.webservices.core.config.service.BaseServiceConfig;
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

public abstract class ServiceHolder<T extends BaseServiceConfig> {
    private static final Logger LOGGER = LogManager.getLogger(ServiceHolder.class);
    private static final int MAX_TERMINATION_TIME_MS = 10000;
    protected final WebserviceDefinition<T> serviceDefinition;
    protected final ServerConfig serverConfig;
    protected final T serviceConfig;
    protected WebserviceBase runningService;
    private boolean inShutdownProcess = false;
    private final List<RunningTask> runningTasks = new ArrayList<>();


    public ServiceHolder(WebserviceDefinition<T> serviceDefinition, ServerConfig serverConfig, BaseServiceConfig serviceConfig) {
        this.serviceDefinition = serviceDefinition;
        this.serverConfig = serverConfig;
        this.serviceConfig = serviceDefinition.getConfigClass().cast(serviceConfig);
    }

    protected abstract void startupServerImpl(WebserviceBase webservice, String instanceName) throws IOException;

    protected abstract void shutdownServerImpl();

    protected abstract boolean isServerRunning();

    public final boolean isRunning() {
        synchronized (this) {
            return runningService != null && isServerRunning();
        }
    }

    public final void startup() throws IOException {
        synchronized (this) {
            if (isRunning()) throw new IllegalStateException("Already running!");

            String instanceName = serviceConfig.getInstanceName();
            LOGGER.info("Starting up {}", instanceName);

            WebserviceBase webservice = serviceDefinition.createNew(serviceConfig, instanceName);
            startupServerImpl(webservice, instanceName);
            this.runningService = webservice;

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

    public final void shutdown() {
        synchronized (this) {
            if (!isRunning()) throw new IllegalStateException("Server already shutdown!");
            initiateShutdown();
        }
    }

    public final void shutdownIfPossible() {
        synchronized (this) {
            if (runningService != null) {
                initiateShutdown();
            }
        }
    }

    private void initiateShutdown() {
        try {
            String instanceName = getInstanceName();
            LOGGER.info("Shutting down {}", instanceName);
            inShutdownProcess = true;

            shutdownServerImpl();

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
        } finally {
            inShutdownProcess = false;
        }
    }

    public final WebserviceBase getServiceIfRunning() {
        synchronized (this) {
            if (isRunning()) return runningService;
            else return null;
        }
    }

    public final WebserviceDefinition<?> getServiceDefinition() {
        return serviceDefinition;
    }

    public final String getInstanceName() {
        return serviceConfig.getInstanceName();
    }

    public final T getServiceConfiguration() {
        return this.serviceConfig;
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
