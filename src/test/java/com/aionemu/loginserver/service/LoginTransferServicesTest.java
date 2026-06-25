package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginTransferServicesTest {

    @Test
    void usesSpringProviderBeforeLegacySingletonFallback() {
        PlayerTransferService playerTransferService = instance(PlayerTransferService.class);
        LoginTransferServices services = new LoginTransferServices(
            provider(PlayerTransferService.class, playerTransferService)
        );

        try {
            assertSame(playerTransferService, LoginTransferServices.playerTransferService());
        } finally {
            services.destroy();
        }
    }

    @Test
    void playerTransferPacketUsesTransferBridgeInsteadOfDirectSingleton() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/clientpackets/CM_PTRANSFER_CONTROL.java"));

        assertFalse(source.contains("PlayerTransferService.getInstance()"));
        assertTrue(source.contains("LoginTransferServices.playerTransferService().requestTransfer"));
        assertTrue(source.contains("LoginTransferServices.playerTransferService().onError"));
        assertTrue(source.contains("LoginTransferServices.playerTransferService().onOk"));
        assertTrue(source.contains("LoginTransferServices.playerTransferService().onTaskStop"));
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
