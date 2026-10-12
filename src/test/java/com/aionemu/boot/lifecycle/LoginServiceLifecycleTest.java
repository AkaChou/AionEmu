package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyLoginConfigOverrides;
import com.aionemu.boot.config.LegacyLoginProperties;
import com.aionemu.loginserver.lifecycle.LoginStartupSequence;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.*;

class LoginServiceLifecycleTest {

    @TempDir
    Path loginConfig;

    @TempDir
    Path loginData;

    @AfterEach
    void clearProperties() {
        System.clearProperty("aion.config.dir");
        System.clearProperty("aion.login.data.dir");
    }

    @Test
    void startupFailureRunsLoginShutdown() {
        configureLoginPaths();
        List<String> events = new ArrayList<>();
        LoginServiceLifecycle lifecycle = new LoginServiceLifecycle(
            new AionServicesProperties(),
            new RecordingLegacyLoginConfigOverrides(events),
            provider(new RecordingStartupSequence(events, true))
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> lifecycle.start(new DefaultApplicationArguments())
        );

        assertEquals("login failed", thrown.getMessage());
        assertEquals(List.of("apply", "start", "stop"), events);

        lifecycle.stop();

        assertEquals(List.of("apply", "start", "stop"), events);
    }

    @Test
    void stopRunsOnceAfterSuccessfulStartup() {
        configureLoginPaths();
        List<String> events = new ArrayList<>();
        LoginServiceLifecycle lifecycle = new LoginServiceLifecycle(
            new AionServicesProperties(),
            new RecordingLegacyLoginConfigOverrides(events),
            provider(new RecordingStartupSequence(events, false))
        );

        lifecycle.start(new DefaultApplicationArguments("--login=true"));
        lifecycle.stop();
        lifecycle.stop();

        assertEquals(List.of("apply", "start", "stop"), events);
    }

    @Test
    void startAppliesLegacyConfigOverridesBeforeStartingLoginServer() {
        configureLoginPaths();
        List<String> events = new ArrayList<>();
        LoginServiceLifecycle lifecycle = new LoginServiceLifecycle(
            new AionServicesProperties(),
            new RecordingLegacyLoginConfigOverrides(events),
            provider(new RecordingStartupSequence(events, false))
        );

        lifecycle.start(new DefaultApplicationArguments("--login=true"));

        assertEquals(List.of("apply", "start"), events);
    }

    /**
     * 单实例 ObjectProvider 测试替身。
     * Single-instance ObjectProvider test double.
     */
    private static ObjectProvider<LoginStartupSequence> provider(LoginStartupSequence sequence) {
        return new ObjectProvider<>() {
            @Override
            public LoginStartupSequence getObject(Object... args) {
                return sequence;
            }

            @Override
            public LoginStartupSequence getIfAvailable() {
                return sequence;
            }

            @Override
            public LoginStartupSequence getObject() {
                return sequence;
            }
        };
    }

    private void configureLoginPaths() {
        System.setProperty("aion.config.dir", loginConfig.toString());
        System.setProperty("aion.login.data.dir", loginData.toString());
    }

    private static final class RecordingLegacyLoginConfigOverrides extends LegacyLoginConfigOverrides {

        private final List<String> events;

        private RecordingLegacyLoginConfigOverrides(List<String> events) {
            super(new LegacyLoginProperties());
            this.events = events;
        }

        @Override
        public void applyToLoginConfig() {
            events.add("apply");
        }
    }

    private static final class RecordingStartupSequence extends LoginStartupSequence {

        private final List<String> events;
        private final boolean failOnStart;

        private RecordingStartupSequence(List<String> events, boolean failOnStart) {
            super(null, null, null, null, null, null, null);
            this.events = events;
            this.failOnStart = failOnStart;
        }

        @Override
        public synchronized void start() {
            events.add("start");
            if (failOnStart) {
                throw new IllegalStateException("login failed");
            }
        }

        @Override
        public synchronized void stop() {
            events.add("stop");
        }
    }
}
