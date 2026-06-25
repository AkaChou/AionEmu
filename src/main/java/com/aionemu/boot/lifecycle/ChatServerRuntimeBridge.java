package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServer;
import com.aionemu.chatserver.ShutdownHook;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class ChatServerRuntimeBridge {

    public void start(String[] args) {
        ChatServer.start(args);
    }

    public void shutdown(boolean restart) {
        ShutdownHook.getInstance().shutdown(restart);
    }
}
