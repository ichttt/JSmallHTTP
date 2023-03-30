package de.nocoffeetech.webservices.core.internal.service;

import de.nocoffeetech.webservices.core.service.ContinuousBackgroundTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class RunningTask {
    private static final Logger LOGGER = LogManager.getLogger(RunningTask.class);
    private final String serviceName;
    private final Thread thread;
    private final ContinuousBackgroundTask task;

    public RunningTask(String serviceName, ContinuousBackgroundTask task, Consumer<String> onTermination) {
        this.serviceName = serviceName;
        this.thread = TaskWrapper.createAndStartThread(serviceName, task, () -> onTermination.accept(task.name()));
        this.task = task;
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

    public void shutdown() {
        task.shutdownAndTerminate(thread);
    }

    public void awaitTermination(long timeoutMillis) {
        try {
            thread.join(timeoutMillis);
        } catch (InterruptedException e) {
            LOGGER.warn("Shutdown thread interrupted while waiting for thread!");
        }
        if (thread.isAlive()) {
            LOGGER.warn("Task {} of service {} did not terminate in time!", name(), serviceName);
        }
    }

    public String name() {
        return task.name();
    }
}
