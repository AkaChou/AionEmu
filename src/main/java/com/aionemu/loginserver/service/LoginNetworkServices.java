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
            return NetConnector.getInstance();
        }
        return provider.getIfAvailable(NetConnector::getInstance);
    }

    @Override
    public void destroy() {
        serverTransportProvider = null;
    }
}
