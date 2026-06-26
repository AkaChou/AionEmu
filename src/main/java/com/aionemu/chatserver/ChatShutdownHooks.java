package com.aionemu.chatserver;

final class ChatShutdownHooks {

    private ChatShutdownHooks() {
    }

    static ShutdownHook shutdownHook(ChatProcessRuntimeBridge processBridge) {
        ShutdownHook shutdownHook = Fallbacks.SHUTDOWN_HOOK;
        shutdownHook.configure(processBridge, null, null);
        return shutdownHook;
    }

    private static final class Fallbacks {

        private static final ShutdownHook SHUTDOWN_HOOK = new ShutdownHook();
    }
}
