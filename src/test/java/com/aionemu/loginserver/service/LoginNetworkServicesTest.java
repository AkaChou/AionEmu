package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.network.ServerTransport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginNetworkServicesTest {

    @Test
    void usesSpringProviderBeforeLegacySingletonFallback() {
        ServerTransport serverTransport = new RecordingTransport();
        LoginNetworkServices services = new LoginNetworkServices(
            provider(ServerTransport.class, serverTransport)
        );

        try {
            assertSame(serverTransport, LoginNetworkServices.serverTransport());
        } finally {
            services.destroy();
        }
    }

    @Test
    void startupNetworkConnectUsesTransportBridgeInsteadOfDirectSingleton() throws IOException {
        String startupBridgeSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/lifecycle/LoginStartupRuntimeBridge.java"));

        assertFalse(startupBridgeSource.contains("NetConnector.getInstance().connect()"));
        assertTrue(startupBridgeSource.contains("LoginNetworkServices.serverTransport().connect()"));
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static final class RecordingTransport implements ServerTransport {

        @Override
        public void connect() {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public int getActiveConnections() {
            return 0;
        }
    }
}
