package de.nocoffeetech.webservices.core.internal.service;

import de.nocoffeetech.webservices.core.service.ContinuousBackgroundTask;

public class TaskWrapper implements Runnable {
    private final Runnable task;
    private final Runnable callback;

    public static Thread createAndStartThread(String serviceName, ContinuousBackgroundTask backgroundTask, Runnable callback) {
        TaskWrapper wrappedTask = new TaskWrapper(backgroundTask, callback);

        Thread thread = new Thread(wrappedTask);
        thread.setName(serviceName + " task \"" + backgroundTask.name() + "\"");
        thread.setPriority(backgroundTask.priority());
        thread.setDaemon(true); //TODO maybe better shutdown management
        thread.start();
        return thread;
    }

    private TaskWrapper(Runnable task, Runnable callback) {
        this.task = task;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            task.run();
        } finally {
            callback.run();
        }
    }
}
