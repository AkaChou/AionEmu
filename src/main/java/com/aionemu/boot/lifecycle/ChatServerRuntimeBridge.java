package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServer;
import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ChatServerRuntime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class ChatServerRuntimeBridge {

    private ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider;
    private ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider;

    @Autowired(required = false)
    void setChatServerRuntimeProvider(ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider) {
        this.chatServerRuntimeProvider = chatServerRuntimeProvider;
    }

    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
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
        processBridge().shutdown(restart);
    }

    private ChatServerRuntime chatServerRuntime() {
        if (chatServerRuntimeProvider == null) {
            return null;
        }
        return chatServerRuntimeProvider.getIfAvailable();
    }

    private ChatProcessRuntimeBridge processBridge() {
        if (processBridgeProvider == null) {
            return new ChatProcessRuntimeBridge();
        }
        return processBridgeProvider.getIfAvailable(ChatProcessRuntimeBridge::new);
    }
}
