package com.aionemu.loginserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.utils.BruteForceProtector;
import com.aionemu.loginserver.utils.FloodProtector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class LoginProtectionServicesTest {

    @Test
    void usesSpringProvidersBeforeLocalFallbacks() {
        BannedMacManager bannedMacManager = instance(BannedMacManager.class);
        LoginBannedIpService bannedIpService = new LoginBannedIpService();
        BruteForceProtector bruteForceProtector = new BruteForceProtector();
        FloodProtector floodProtector = new FloodProtector();
        LoginProtectionServices services = new LoginProtectionServices(
            provider(BannedMacManager.class, bannedMacManager),
            provider(LoginBannedIpService.class, bannedIpService),
            provider(BruteForceProtector.class, bruteForceProtector),
            provider(FloodProtector.class, floodProtector)
        );

        try {
            assertSame(bannedMacManager, LoginProtectionServices.bannedMacManager());
            assertSame(bannedIpService, LoginProtectionServices.bannedIpService());
            assertSame(bruteForceProtector, LoginProtectionServices.bruteForceProtector());
            assertSame(floodProtector, LoginProtectionServices.floodProtector());
        } finally {
            services.destroy();
        }
    }

    @Test
    void protectionBridgeUsesLocalFallbacksInsteadOfDirectLegacySingletons() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/service/LoginProtectionServices.java"));

        assertFalse(source.contains("BannedMacManager.getInstance()"));
        assertFalse(source.contains("BruteForceProtector.getInstance()"));
        assertFalse(source.contains("FloodProtector.getInstance()"));
        assertTrue(source.contains("Fallbacks.BANNED_MAC_MANAGER"));
        assertTrue(source.contains("Fallbacks.BRUTE_FORCE_PROTECTOR"));
        assertTrue(source.contains("Fallbacks.FLOOD_PROTECTOR"));
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

    @Test
    void bannedIpAccessUsesProtectionBridgeInsteadOfDirectControllerCalls() throws IOException {
        String startupBridgeSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/lifecycle/LoginStartupRuntimeBridge.java"));
        String accountControllerSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/controller/AccountController.java"));
        String loginPacketSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/aion/clientpackets/CM_LOGIN.java"));
        String banPacketSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/clientpackets/CM_BAN.java"));
        String floodProtectorSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/utils/FloodProtector.java"));

        assertFalse(startupBridgeSource.contains("BannedIpController."));
        assertTrue(startupBridgeSource.contains("LoginProtectionServices.bannedIpService().start()"));

        assertFalse(accountControllerSource.contains("BannedIpController."));
        assertTrue(accountControllerSource.contains("LoginProtectionServices.bannedIpService().isBanned"));

        assertFalse(loginPacketSource.contains("BannedIpController."));
        assertTrue(loginPacketSource.contains("LoginProtectionServices.bannedIpService().banIp"));

        assertFalse(banPacketSource.contains("BannedIpController."));
        assertTrue(banPacketSource.contains("LoginProtectionServices.bannedIpService().unbanIp"));
        assertTrue(banPacketSource.contains("LoginProtectionServices.bannedIpService().banIp"));

        assertFalse(floodProtectorSource.contains("BannedIpController."));
        assertTrue(floodProtectorSource.contains("LoginProtectionServices.bannedIpService().isBanned"));
        assertTrue(floodProtectorSource.contains("LoginProtectionServices.bannedIpService().banIp"));
    }

    @Test
    void bannedMacAccessUsesProtectionBridgeInsteadOfDirectControllerCalls() throws IOException {
        String macBanPacketSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/clientpackets/CM_MACBAN_CONTROL.java"));
        String macBanListSource = Files.readString(Path.of("src/main/java/com/aionemu/loginserver/network/gameserver/serverpackets/SM_MACBAN_LIST.java"));

        assertFalse(macBanPacketSource.contains("BannedMacManager.getInstance()"));
        assertTrue(macBanPacketSource.contains("LoginProtectionServices.bannedMacManager().unban"));
        assertTrue(macBanPacketSource.contains("LoginProtectionServices.bannedMacManager().ban"));

        assertFalse(macBanListSource.contains("BannedMacManager.getInstance()"));
        assertTrue(macBanListSource.contains("LoginProtectionServices.bannedMacManager().getMap()"));
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
