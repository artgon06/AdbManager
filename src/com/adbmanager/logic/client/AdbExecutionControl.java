package com.adbmanager.logic.client;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class AdbExecutionControl {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicLong lastActivityNanos = new AtomicLong(System.nanoTime());

    public void markActivity() {
        lastActivityNanos.set(System.nanoTime());
    }

    public long lastActivityNanos() {
        return lastActivityNanos.get();
    }

    public void cancel() {
        cancelled.set(true);
        markActivity();
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
