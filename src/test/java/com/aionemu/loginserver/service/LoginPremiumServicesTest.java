package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.controller.PremiumController;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginPremiumServicesTest {

    @Test
    void usesSpringProviderBeforeLegacySingletonFallback() {
        PremiumController premiumController = instance(PremiumController.class);
        LoginPremiumServices services = new LoginPremiumServices(
            provider(PremiumController.class, premiumController)
        );

        try {
            assertSame(premiumController, LoginPremiumServices.premiumController());
        } finally {
            services.destroy();
        }
    }

    @Test
    void startupRuntimeBridgeUsesPremiumBridgeInsteadOfDirectSingleton() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/lifecycle/LoginStartupRuntimeBridge.java"));

        assertFalse(source.contains("PremiumController.getController()"));
        assertFalse(source.contains("premiumControllerProvider"));
        assertTrue(source.contains("LoginPremiumServices.premiumController()"));
    }

    @Test
    void premiumPacketUsesPremiumBridgeInsteadOfDirectSingleton() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/clientpackets/CM_PREMIUM_CONTROL.java"));

        assertFalse(source.contains("PremiumController.getController()"));
        assertTrue(source.contains("LoginPremiumServices.premiumController().requestBuy"));
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
