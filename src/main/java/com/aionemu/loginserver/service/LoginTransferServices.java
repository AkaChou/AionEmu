package com.aionemu.loginserver.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginTransferServices implements DisposableBean {

    private static volatile ObjectProvider<PlayerTransferService> playerTransferServiceProvider;

    public LoginTransferServices(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        LoginTransferServices.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    public static PlayerTransferService playerTransferService() {
        ObjectProvider<PlayerTransferService> provider = playerTransferServiceProvider;
        if (provider == null) {
            return fallbackPlayerTransferService();
        }
        return provider.getIfAvailable(LoginTransferServices::fallbackPlayerTransferService);
    }

    @Override
    public void destroy() {
        playerTransferServiceProvider = null;
    }

    private static PlayerTransferService fallbackPlayerTransferService() {
        return Fallbacks.PLAYER_TRANSFER_SERVICE;
    }

    private static final class Fallbacks {

        private static final PlayerTransferService PLAYER_TRANSFER_SERVICE = new PlayerTransferService();
    }
}
