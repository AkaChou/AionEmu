package com.aionemu.commons.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class AionEmbeddedShutdownHandler {

    private static final AtomicReference<Registration> HANDLER = new AtomicReference<>();

    private AionEmbeddedShutdownHandler() {
    }

    public static void register(Runnable handler) {
        Objects.requireNonNull(handler, "handler");
        HANDLER.set(new Registration(handler, ignored -> handler.run()));
    }

    public static void register(Consumer<AionEmbeddedShutdownMode> handler) {
        Objects.requireNonNull(handler, "handler");
        HANDLER.set(new Registration(handler, handler));
    }

    public static void clear(Runnable handler) {
        clearByKey(handler);
    }

    public static void clear(Consumer<AionEmbeddedShutdownMode> handler) {
        clearByKey(handler);
    }

    public static void clear() {
        HANDLER.set(null);
    }

    public static boolean requestShutdown() {
        return requestShutdown(AionEmbeddedShutdownMode.SHUTDOWN);
    }

    public static boolean requestShutdown(AionEmbeddedShutdownMode mode) {
        Registration registration = HANDLER.get();
        if (registration == null) {
            return false;
        }
        registration.handler().accept(Objects.requireNonNull(mode, "mode"));
        return true;
    }

    private static void clearByKey(Object handler) {
        Registration registration = HANDLER.get();
        if (registration != null && registration.key() == handler) {
            HANDLER.compareAndSet(registration, null);
        }
    }

    private record Registration(Object key, Consumer<AionEmbeddedShutdownMode> handler) {
    }
}
