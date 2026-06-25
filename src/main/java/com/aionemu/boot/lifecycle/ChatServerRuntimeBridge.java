package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServer;
import com.aionemu.chatserver.ChatServerRuntime;
import com.aionemu.chatserver.ShutdownHook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class ChatServerRuntimeBridge {

    private ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider;

    @Autowired(required = false)
    void setChatServerRuntimeProvider(ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider) {
        this.chatServerRuntimeProvider = chatServerRuntimeProvider;
    }

    public void start(String[] args) {
        ChatServerRuntime chatServerRuntime = chatServerRuntime();
        if (chatServerRuntime != null) {
            chatServerRuntime.start(args);
            return;
        }
        ChatServer.start(args);
    }

    public void shutdown(boolean restart) {
        ShutdownHook.getInstance().shutdown(restart);
    }

    private ChatServerRuntime chatServerRuntime() {
        if (chatServerRuntimeProvider == null) {
            return null;
        }
        return chatServerRuntimeProvider.getIfAvailable();
    }
}
