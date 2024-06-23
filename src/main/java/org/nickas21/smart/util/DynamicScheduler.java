package org.nickas21.smart.util;

import org.nickas21.smart.DefaultSmartSolarmanTuyaService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DynamicScheduler {
    private final ScheduledExecutorService executorService;
    private ScheduledFuture<?> scheduledFuture;
    private long timeoutSecUpdate;
    private final DefaultSmartSolarmanTuyaService serviceInstance;

    public DynamicScheduler(long initialTimeoutSecUpdate, DefaultSmartSolarmanTuyaService serviceInstance) {
        this.timeoutSecUpdate = initialTimeoutSecUpdate;
        this.serviceInstance = serviceInstance;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        scheduleTask();
    }

    private void scheduleTask() {
        // Cancel the existing scheduled task if it exists
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(false);
        }

        // Schedule the task with the new period
        scheduledFuture = executorService.scheduleAtFixedRate(serviceInstance::setBmsSocCur, 0, timeoutSecUpdate, TimeUnit.SECONDS);
    }

    public void updateTimeoutSecUpdate(long newTimeoutSecUpdate) {
        this.timeoutSecUpdate = newTimeoutSecUpdate;
        scheduleTask(); // Reschedule the task with the new period
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
