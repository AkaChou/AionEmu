package com.aionemu.chatserver.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public final class ChatRestartServices implements DisposableBean {

    private static volatile ObjectProvider<RestartService> restartServiceProvider;

    public ChatRestartServices(ObjectProvider<RestartService> restartServiceProvider) {
        ChatRestartServices.restartServiceProvider = restartServiceProvider;
    }

    public static RestartService restartService() {
        ObjectProvider<RestartService> provider = restartServiceProvider;
        if (provider == null) {
            return fallbackRestartService();
        }
        return provider.getIfAvailable(ChatRestartServices::fallbackRestartService);
    }

    @Override
    public void destroy() {
        restartServiceProvider = null;
    }

    private static RestartService fallbackRestartService() {
        return Fallbacks.RESTART_SERVICE;
    }

    private static final class Fallbacks {

        private static final RestartService RESTART_SERVICE = new RestartService();
    }
}
