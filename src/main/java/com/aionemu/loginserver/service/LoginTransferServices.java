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
            return PlayerTransferService.getInstance();
        }
        return provider.getIfAvailable(PlayerTransferService::getInstance);
    }

    @Override
    public void destroy() {
        playerTransferServiceProvider = null;
    }
}
