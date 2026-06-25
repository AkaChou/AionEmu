package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.utils.BruteForceProtector;
import com.aionemu.loginserver.utils.FloodProtector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginProtectionServicesTest {

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        BruteForceProtector bruteForceProtector = new BruteForceProtector();
        FloodProtector floodProtector = new FloodProtector();
        LoginProtectionServices services = new LoginProtectionServices(
            provider(BruteForceProtector.class, bruteForceProtector),
            provider(FloodProtector.class, floodProtector)
        );

        try {
            assertSame(bruteForceProtector, LoginProtectionServices.bruteForceProtector());
            assertSame(floodProtector, LoginProtectionServices.floodProtector());
        } finally {
            services.destroy();
        }
    }

    @Test
    void networkPacketCodeUsesProtectionBridgeInsteadOfDirectSingletons() throws IOException {
        String connectionFactorySource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/aion/AionConnectionFactoryImpl.java"));
        String loginPacketSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/aion/clientpackets/CM_LOGIN.java"));

        assertFalse(connectionFactorySource.contains("FloodProtector.getInstance()"));
        assertTrue(connectionFactorySource.contains("LoginProtectionServices.floodProtector()"));
        assertFalse(loginPacketSource.contains("BruteForceProtector.getInstance()"));
        assertTrue(loginPacketSource.contains("LoginProtectionServices.bruteForceProtector()"));
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
