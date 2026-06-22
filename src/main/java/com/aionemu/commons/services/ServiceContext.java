package com.aionemu.commons.services;

import java.util.Locale;

public final class ServiceContext {

    private static final String DEFAULT_CONTEXT = "default";
    private static final InheritableThreadLocal<String> CURRENT = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return DEFAULT_CONTEXT;
        }
    };

    private ServiceContext() {
    }

    public static String current() {
        return CURRENT.get();
    }

    public static Scope use(String name) {
        String previous = CURRENT.get();
        CURRENT.set(normalize(name));
        return new Scope(previous);
    }

    public static Runnable wrap(Runnable runnable) {
        return wrap(runnable, current());
    }

    public static Runnable wrap(Runnable runnable, String context) {
        String normalizedContext = normalize(context);
        return new Runnable() {
            @Override
            public void run() {
                try (Scope ignored = use(normalizedContext)) {
                    runnable.run();
                }
            }
        };
    }

    private static String normalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_CONTEXT;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Scope implements AutoCloseable {
        private final String previous;
        private boolean closed;

        private Scope(String previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (!closed) {
                CURRENT.set(previous);
                closed = true;
            }
        }
    }
}
