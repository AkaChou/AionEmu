package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.Shutdown;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginShutdownServicesTest {

    @Test
    void usesSpringProviderBeforeLegacySingletonFallback() {
        RecordingShutdown shutdown = new RecordingShutdown();
        LoginShutdownServices services = new LoginShutdownServices(provider(Shutdown.class, shutdown));

        try {
            assertSame(shutdown, LoginShutdownServices.shutdown());
        } finally {
            services.destroy();
        }
    }

    @Test
    void shutdownPathsUseSharedBridgeInsteadOfDirectSingleton() throws IOException {
        String requestSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginShutdownRequest.java"));
        String processSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/lifecycle/LoginProcessRuntimeBridge.java"));

        assertFalse(requestSource.contains("Shutdown.getInstance()"));
        assertFalse(processSource.contains("Shutdown.getInstance()"));
        assertTrue(requestSource.contains("LoginShutdownServices.shutdown()"));
        assertTrue(processSource.contains("LoginShutdownServices.shutdown()"));
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static final class RecordingShutdown extends Shutdown {
        @Override
        public void shutdown(boolean haltJvm) {
            // no-op
        }
    }
}
