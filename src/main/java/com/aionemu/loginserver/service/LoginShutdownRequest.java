package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class LoginShutdownRequest implements DisposableBean {

    private static volatile ObjectProvider<Shutdown> shutdownProvider;

    @Autowired
    public LoginShutdownRequest(ObjectProvider<Shutdown> shutdownProvider) {
        setShutdownProvider(shutdownProvider);
    }

    public static void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        LoginShutdownRequest.shutdownProvider = shutdownProvider;
    }

    public static void shutdownWithoutHalt() {
        shutdown().shutdown(false);
    }

    public static void startShutdown(boolean restartOnly) {
        Shutdown shutdown = shutdown();
        shutdown.setRestartOnly(restartOnly);
        shutdown.start();
    }

    @Override
    public void destroy() {
        shutdownProvider = null;
    }

    private static Shutdown shutdown() {
        ObjectProvider<Shutdown> provider = shutdownProvider;
        if (provider == null) {
            return Shutdown.getInstance();
        }
        return provider.getIfAvailable(Shutdown::getInstance);
    }
}
