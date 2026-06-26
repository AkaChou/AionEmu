package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ShutdownHook;

final class GameShutdownHookFallbacks {

    private GameShutdownHookFallbacks() {
    }

    static ShutdownHook shutdownHook() {
        return ShutdownHookFallback.INSTANCE;
    }

    private static final class ShutdownHookFallback {
        private static final ShutdownHook INSTANCE = ShutdownHook.getInstance();
    }
}
