package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class GameShutdownRequest implements DisposableBean {

    private static volatile ObjectProvider<ShutdownHook> shutdownHookProvider;

    @Autowired
    public GameShutdownRequest(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        setShutdownHookProvider(shutdownHookProvider);
    }

    public static void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        GameShutdownRequest.shutdownHookProvider = shutdownHookProvider;
    }

    public static void doShutdown(int delay, int announceInterval, ShutdownMode mode) {
        shutdownHook().doShutdown(delay, announceInterval, mode);
    }

    public static void completeShutdown(ShutdownMode mode, boolean haltRuntime) {
        shutdownHook().completeShutdown(mode, haltRuntime);
    }

    @Override
    public void destroy() {
        shutdownHookProvider = null;
    }

    private static ShutdownHook shutdownHook() {
        ObjectProvider<ShutdownHook> provider = shutdownHookProvider;
        if (provider == null) {
            return ShutdownHook.getInstance();
        }
        return provider.getIfAvailable(ShutdownHook::getInstance);
    }
}
