package com.aionemu.loginserver.service;

import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.network.NetConnector;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginNetworkServices implements DisposableBean {

    private static volatile ObjectProvider<ServerTransport> serverTransportProvider;

    public LoginNetworkServices(ObjectProvider<ServerTransport> serverTransportProvider) {
        LoginNetworkServices.serverTransportProvider = serverTransportProvider;
    }

    public static ServerTransport serverTransport() {
        ObjectProvider<ServerTransport> provider = serverTransportProvider;
        if (provider == null) {
            return fallbackServerTransport();
        }
        return provider.getIfAvailable(LoginNetworkServices::fallbackServerTransport);
    }

    @Override
    public void destroy() {
        serverTransportProvider = null;
    }

    private static ServerTransport fallbackServerTransport() {
        return Fallbacks.currentTransport();
    }

    private static final class Fallbacks {

        private static ServerTransport currentTransport() {
            return NetConnector.currentTransport();
        }
    }
}
