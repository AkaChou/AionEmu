package com.aionemu.boot.lifecycle;

import org.springframework.stereotype.Component;

@Component
public class ChatServerLifecycleGateway {

    public void start(String[] args) {
        com.aionemu.chatserver.ChatServer.start(args);
    }

    public void stop() {
        com.aionemu.chatserver.ShutdownHook.getInstance().shutdown(false);
    }
}
