package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginShutdownServices implements DisposableBean {

    private static volatile ObjectProvider<Shutdown> shutdownProvider;
    private static volatile Shutdown resolvedShutdown;

    public LoginShutdownServices(ObjectProvider<Shutdown> shutdownProvider) {
        setShutdownProvider(shutdownProvider);
    }

    public static void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        LoginShutdownServices.shutdownProvider = shutdownProvider;
    }

    public static Shutdown shutdown() {
        ObjectProvider<Shutdown> provider = shutdownProvider;
        if (provider == null) {
            Shutdown resolved = resolvedShutdown;
            if (resolved != null) {
                return resolved;
            }
            return remember(fallbackShutdown());
        }
        return remember(provider.getIfAvailable(LoginShutdownServices::fallbackShutdown));
    }

    @Override
    public void destroy() {
        shutdownProvider = null;
    }

    private static Shutdown fallbackShutdown() {
        return Fallbacks.SHUTDOWN;
    }

    private static Shutdown remember(Shutdown shutdown) {
        resolvedShutdown = shutdown;
        return shutdown;
    }

    public static void resetForTests() {
        shutdownProvider = null;
        resolvedShutdown = null;
    }

    private static final class Fallbacks {

        private static final Shutdown SHUTDOWN = new Shutdown();
    }
}
