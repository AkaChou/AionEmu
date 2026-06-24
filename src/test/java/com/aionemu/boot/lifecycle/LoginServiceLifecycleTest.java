package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.boot.config.AionServicesProperties;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    private static Class<?> fieldType(String name) {
        try {
            return LoginServiceLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }
}
