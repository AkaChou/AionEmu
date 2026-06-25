package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.utils.ThreadPoolManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginThreadPoolServicesTest {

    @Test
    void usesSpringProviderBeforeLegacySingletonFallback() {
        ThreadPoolManager threadPoolManager = instance(ThreadPoolManager.class);
        LoginThreadPoolServices services = new LoginThreadPoolServices(
            provider(ThreadPoolManager.class, threadPoolManager)
        );

        try {
            assertSame(threadPoolManager, LoginThreadPoolServices.threadPoolManager());
        } finally {
            services.destroy();
        }
    }

    @Test
    void gameServerConnectionCodeUsesThreadPoolBridgeInsteadOfDirectSingleton() throws IOException {
        String connectionSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/GsConnection.java"));
        String authPacketSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/clientpackets/CM_GS_AUTH.java"));

        assertFalse(connectionSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(connectionSource.contains("LoginThreadPoolServices.threadPoolManager().executeLsPacket"));
        assertTrue(connectionSource.contains("LoginThreadPoolServices.threadPoolManager().schedule"));

        assertFalse(authPacketSource.contains("ThreadPoolManager.getInstance()"));
        assertTrue(authPacketSource.contains("LoginThreadPoolServices.threadPoolManager().schedule"));
    }

    private static <T> T instance(Class<T> type) {
        return new ObjenesisStd().newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
