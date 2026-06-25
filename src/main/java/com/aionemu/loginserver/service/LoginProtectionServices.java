package com.aionemu.loginserver.service;

import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.utils.BruteForceProtector;
import com.aionemu.loginserver.utils.FloodProtector;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginProtectionServices implements DisposableBean {

    private static volatile ObjectProvider<BannedMacManager> bannedMacManagerProvider;
    private static volatile ObjectProvider<LoginBannedIpService> bannedIpServiceProvider;
    private static volatile ObjectProvider<BruteForceProtector> bruteForceProtectorProvider;
    private static volatile ObjectProvider<FloodProtector> floodProtectorProvider;

    public LoginProtectionServices(
        ObjectProvider<BannedMacManager> bannedMacManagerProvider,
        ObjectProvider<LoginBannedIpService> bannedIpServiceProvider,
        ObjectProvider<BruteForceProtector> bruteForceProtectorProvider,
        ObjectProvider<FloodProtector> floodProtectorProvider
    ) {
        LoginProtectionServices.bannedMacManagerProvider = bannedMacManagerProvider;
        LoginProtectionServices.bannedIpServiceProvider = bannedIpServiceProvider;
        LoginProtectionServices.bruteForceProtectorProvider = bruteForceProtectorProvider;
        LoginProtectionServices.floodProtectorProvider = floodProtectorProvider;
    }

    public static BannedMacManager bannedMacManager() {
        ObjectProvider<BannedMacManager> provider = bannedMacManagerProvider;
        if (provider == null) {
            return fallbackBannedMacManager();
        }
        return provider.getIfAvailable(LoginProtectionServices::fallbackBannedMacManager);
    }

    public static LoginBannedIpService bannedIpService() {
        ObjectProvider<LoginBannedIpService> provider = bannedIpServiceProvider;
        if (provider == null) {
            return new LoginBannedIpService();
        }
        return provider.getIfAvailable(LoginBannedIpService::new);
    }

    public static BruteForceProtector bruteForceProtector() {
        ObjectProvider<BruteForceProtector> provider = bruteForceProtectorProvider;
        if (provider == null) {
            return fallbackBruteForceProtector();
        }
        return provider.getIfAvailable(LoginProtectionServices::fallbackBruteForceProtector);
    }

    public static FloodProtector floodProtector() {
        ObjectProvider<FloodProtector> provider = floodProtectorProvider;
        if (provider == null) {
            return fallbackFloodProtector();
        }
        return provider.getIfAvailable(LoginProtectionServices::fallbackFloodProtector);
    }

    @Override
    public void destroy() {
        bannedMacManagerProvider = null;
        bannedIpServiceProvider = null;
        bruteForceProtectorProvider = null;
        floodProtectorProvider = null;
    }

    private static BannedMacManager fallbackBannedMacManager() {
        return Fallbacks.BANNED_MAC_MANAGER;
    }

    private static BruteForceProtector fallbackBruteForceProtector() {
        return Fallbacks.BRUTE_FORCE_PROTECTOR;
    }

    private static FloodProtector fallbackFloodProtector() {
        return Fallbacks.FLOOD_PROTECTOR;
    }

    private static final class Fallbacks {

        private static final BannedMacManager BANNED_MAC_MANAGER = new BannedMacManager();
        private static final BruteForceProtector BRUTE_FORCE_PROTECTOR = new BruteForceProtector();
        private static final FloodProtector FLOOD_PROTECTOR = new FloodProtector();
    }
}
