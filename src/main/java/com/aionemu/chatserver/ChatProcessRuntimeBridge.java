package com.aionemu.chatserver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatProcessRuntimeBridge {

    public Thread shutdownHook() {
        return ShutdownHook.getInstance(this);
    }

    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void shutdown(boolean restart) {
        ShutdownHook.getInstance(this).shutdown(restart);
    }

    public void halt(int status) {
        Runtime.getRuntime().halt(status);
    }
}
