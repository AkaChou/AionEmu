package com.aionemu.commons.utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class AionEmbeddedFailureHandler {

    private static final AtomicReference<Consumer<RuntimeException>> HANDLER = new AtomicReference<>();

    private AionEmbeddedFailureHandler() {
    }

    public static void register(Consumer<RuntimeException> handler) {
        HANDLER.set(handler);
    }

    public static void clear(Consumer<RuntimeException> handler) {
        HANDLER.compareAndSet(handler, null);
    }

    public static void clear() {
        HANDLER.set(null);
    }

    public static void fail(RuntimeException failure) {
        Consumer<RuntimeException> handler = HANDLER.get();
        if (handler != null) {
            handler.accept(failure);
            return;
        }
        throw failure;
    }
}
