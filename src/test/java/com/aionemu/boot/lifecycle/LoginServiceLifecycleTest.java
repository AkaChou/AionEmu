package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.loginserver.lifecycle.LoginProcessRuntimeBridge;
import com.aionemu.loginserver.lifecycle.LoginStartupSequenceLifecycle;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.DefaultApplicationArguments;

class LoginServiceLifecycleTest {

    @TempDir
    Path loginConfig;

    @TempDir
    Path loginData;

    @AfterEach
    void clearProperties() {
        System.clearProperty("aion.login.config.dir");
        System.clearProperty("aion.login.data.dir");
    }

    @Test
    void usesLoginServerLifecycleGateway() {
        assertEquals(LoginServerLifecycleGateway.class, fieldType("loginServerLifecycleGateway"));
        assertEquals(ObjectProvider.class, fieldType(LoginServerLifecycleGateway.class, "startupSequenceLifecycleProvider"));
        assertEquals(ObjectProvider.class, fieldType(LoginServerLifecycleGateway.class, "runtimeBridgeProvider"));
        assertEquals(ObjectProvider.class, fieldType(LoginServerRuntimeBridge.class, "processBridgeProvider"));
        assertEquals(null, findFieldType(LoginServerLifecycleGateway.class, "startupSequenceLifecycle"));
    }

    @Test
    void startupFailureRunsLoginShutdown() {
        configureLoginPaths();
        List<String> events = new ArrayList<>();
        LoginServerLifecycleGateway gateway = new RecordingLoginServerLifecycleGateway(events, true);
        LoginServiceLifecycle lifecycle = new LoginServiceLifecycle(new AionServicesProperties(), gateway);

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> lifecycle.start(new DefaultApplicationArguments())
        );

        assertEquals("login failed", thrown.getMessage());
        assertEquals(List.of("start", "stop"), events);

        lifecycle.stop();

        assertEquals(List.of("start", "stop"), events);
    }

    @Test
    void stopRunsOnceAfterSuccessfulStartup() {
        configureLoginPaths();
        List<String> events = new ArrayList<>();
        LoginServerLifecycleGateway gateway = new RecordingLoginServerLifecycleGateway(events, false);
        LoginServiceLifecycle lifecycle = new LoginServiceLifecycle(new AionServicesProperties(), gateway);

        lifecycle.start(new DefaultApplicationArguments("--login=true"));
        lifecycle.stop();
        lifecycle.stop();

        assertEquals(List.of("start:1", "stop"), events);
    }

    @Test
    void loginGatewayUsesSpringProvidersForRuntimeBridgeAndStartupLifecycle() {
        List<String> events = new ArrayList<>();
        LoginServerLifecycleGateway gateway = new LoginServerLifecycleGateway();
        gateway.setStartupSequenceLifecycleProvider(provider(
            LoginStartupSequenceLifecycle.class,
            new RecordingLoginStartupSequenceLifecycle(events)
        ));
        gateway.setRuntimeBridgeProvider(provider(
            LoginServerRuntimeBridge.class,
            new RecordingLoginServerRuntimeBridge(events)
        ));

        gateway.start(new String[] {"--login=true"});
        gateway.stop();

        assertEquals(List.of("start:1:true", "shutdown:false", "reset"), events);
    }

    @Test
    void loginRuntimeBridgeRoutesShutdownThroughProcessBridgeProvider() {
        List<String> events = new ArrayList<>();
        LoginServerRuntimeBridge runtimeBridge = new LoginServerRuntimeBridge();
        runtimeBridge.setProcessBridgeProvider(provider(
            LoginProcessRuntimeBridge.class,
            new RecordingLoginProcessRuntimeBridge(events)
        ));

        runtimeBridge.shutdown(true);

        assertEquals(List.of("process:shutdown:true"), events);
    }

    @Test
    void loginRuntimeBridgePreparesProcessBridgeBeforeStartingServer() {
        List<String> events = new ArrayList<>();
        LoginServerRuntimeBridge runtimeBridge = new LoginServerRuntimeBridge() {
            @Override
            protected void doStart(String[] args) {
                events.add("start:" + args.length + ":false");
            }

            @Override
            protected void doStart(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
                events.add("start:" + args.length + ":" + (startupSequenceLifecycle != null));
            }
        };
        runtimeBridge.setProcessBridgeProvider(oneShotProvider(new RecordingLoginProcessRuntimeBridge(events)));

        runtimeBridge.start(
            new String[] {"--login=true"},
            new RecordingLoginStartupSequenceLifecycle(new ArrayList<>())
        );
        runtimeBridge.shutdown(false);

        assertEquals(List.of("process:prepare", "start:1:true", "process:shutdown:false"), events);
    }

    private void configureLoginPaths() {
        System.setProperty("aion.login.config.dir", loginConfig.toString());
        System.setProperty("aion.login.data.dir", loginData.toString());
    }

    private static final class RecordingLoginServerLifecycleGateway extends LoginServerLifecycleGateway {

        private final List<String> events;
        private final boolean failOnStart;

        private RecordingLoginServerLifecycleGateway(List<String> events, boolean failOnStart) {
            this.events = events;
            this.failOnStart = failOnStart;
        }

        @Override
        public void start(String[] args) {
            events.add(failOnStart ? "start" : "start:" + args.length);
            if (failOnStart) {
                throw new IllegalStateException("login failed");
            }
        }

        @Override
        public void stop() {
            events.add("stop");
        }
    }

    private static final class RecordingLoginServerRuntimeBridge extends LoginServerRuntimeBridge {

        private final List<String> events;

        private RecordingLoginServerRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public void start(String[] args) {
            events.add("start:" + args.length + ":false");
        }

        @Override
        public void start(String[] args, LoginStartupSequenceLifecycle startupSequenceLifecycle) {
            events.add("start:" + args.length + ":" + (startupSequenceLifecycle != null));
        }

        @Override
        public void shutdown(boolean restart) {
            events.add("shutdown:" + restart);
        }
    }

    private static final class RecordingLoginProcessRuntimeBridge extends LoginProcessRuntimeBridge {

        private final List<String> events;

        private RecordingLoginProcessRuntimeBridge(List<String> events) {
            this.events = events;
        }

        @Override
        public void shutdown(boolean restart) {
            events.add("process:shutdown:" + restart);
        }

        @Override
        public void prepareShutdown() {
            events.add("process:prepare");
        }
    }

    private static final class RecordingLoginStartupSequenceLifecycle extends LoginStartupSequenceLifecycle {

        private final List<String> events;

        private RecordingLoginStartupSequenceLifecycle(List<String> events) {
            super(null);
            this.events = events;
        }

        @Override
        public synchronized void reset() {
            events.add("reset");
        }
    }

    private static Class<?> fieldType(String name) {
        return fieldType(LoginServiceLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> findFieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static <T> ObjectProvider<T> oneShotProvider(T instance) {
        AtomicBoolean used = new AtomicBoolean();
        return ObjectProvider.class.cast(Proxy.newProxyInstance(
            ObjectProvider.class.getClassLoader(),
            new Class<?>[] { ObjectProvider.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "oneShotProvider";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                if ("getIfAvailable".equals(method.getName())) {
                    if (!used.compareAndSet(false, true)) {
                        throw new ProviderUsedAfterPreparationException();
                    }
                    return instance;
                }
                throw new UnsupportedOperationException(method.toString());
            }
        ));
    }

    private static final class ProviderUsedAfterPreparationException extends RuntimeException {
    }
}
