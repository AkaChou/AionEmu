package com.aionemu.loginserver.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginTransferServices implements DisposableBean {

    private static volatile ObjectProvider<PlayerTransferService> playerTransferServiceProvider;
    private static volatile PlayerTransferService resolvedPlayerTransferService;

    public LoginTransferServices(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        LoginTransferServices.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    public static PlayerTransferService playerTransferService() {
        ObjectProvider<PlayerTransferService> provider = playerTransferServiceProvider;
        if (provider == null) {
            PlayerTransferService resolved = resolvedPlayerTransferService;
            if (resolved != null) {
                return resolved;
            }
            return remember(fallbackPlayerTransferService());
        }
        return remember(provider.getIfAvailable(LoginTransferServices::fallbackPlayerTransferService));
    }

    @Override
    public void destroy() {
        playerTransferServiceProvider = null;
    }

    private static PlayerTransferService fallbackPlayerTransferService() {
        return Fallbacks.PLAYER_TRANSFER_SERVICE;
    }

    private static PlayerTransferService remember(PlayerTransferService playerTransferService) {
        resolvedPlayerTransferService = playerTransferService;
        return playerTransferService;
    }

    static void resetForTests() {
        playerTransferServiceProvider = null;
        resolvedPlayerTransferService = null;
    }

    private static final class Fallbacks {

        private static final PlayerTransferService PLAYER_TRANSFER_SERVICE = new PlayerTransferService();
    }
}
