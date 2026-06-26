package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginShutdownServices implements DisposableBean {

    private static volatile ObjectProvider<Shutdown> shutdownProvider;

    public LoginShutdownServices(ObjectProvider<Shutdown> shutdownProvider) {
        setShutdownProvider(shutdownProvider);
    }

    public static void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        LoginShutdownServices.shutdownProvider = shutdownProvider;
    }

    public static Shutdown shutdown() {
        ObjectProvider<Shutdown> provider = shutdownProvider;
        if (provider == null) {
            return fallbackShutdown();
        }
        return provider.getIfAvailable(LoginShutdownServices::fallbackShutdown);
    }

    @Override
    public void destroy() {
        shutdownProvider = null;
    }

    private static Shutdown fallbackShutdown() {
        return Fallbacks.SHUTDOWN;
    }

    private static final class Fallbacks {

        private static final Shutdown SHUTDOWN = new Shutdown();
    }
}
