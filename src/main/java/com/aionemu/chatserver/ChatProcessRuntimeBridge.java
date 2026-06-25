package com.aionemu.chatserver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatProcessRuntimeBridge {

    private ObjectProvider<ShutdownHook> shutdownHookProvider;

    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
    }

    public Thread shutdownHook() {
        return shutdownHookInstance();
    }

    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void shutdown(boolean restart) {
        shutdownHookInstance().shutdown(restart);
    }

    public void halt(int status) {
        Runtime.getRuntime().halt(status);
    }

    private ShutdownHook shutdownHookInstance() {
        if (shutdownHookProvider == null) {
            return ShutdownHook.getInstance(this);
        }
        return shutdownHookProvider.getIfAvailable(() -> ShutdownHook.getInstance(this));
    }
}
