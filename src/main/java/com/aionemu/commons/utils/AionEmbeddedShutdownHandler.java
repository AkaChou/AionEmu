package com.aionemu.commons.utils;

import java.util.concurrent.atomic.AtomicReference;

public final class AionEmbeddedShutdownHandler {

    private static final AtomicReference<Runnable> HANDLER = new AtomicReference<>();

    private AionEmbeddedShutdownHandler() {
    }

    public static void register(Runnable handler) {
        HANDLER.set(handler);
    }

    public static void clear(Runnable handler) {
        HANDLER.compareAndSet(handler, null);
    }

    public static void clear() {
        HANDLER.set(null);
    }

    public static boolean requestShutdown() {
        Runnable handler = HANDLER.get();
        if (handler == null) {
            return false;
        }
        handler.run();
        return true;
    }
}
