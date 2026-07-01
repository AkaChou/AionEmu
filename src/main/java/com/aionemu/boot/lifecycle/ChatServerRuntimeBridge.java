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
    private ChatProcessRuntimeBridge processBridge;

    @Autowired(required = false)
    void setChatServerRuntimeProvider(ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider) {
        this.chatServerRuntimeProvider = chatServerRuntimeProvider;
    }

    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

    public void start(String[] args) {
        prepareShutdown();
        ChatServerRuntime chatServerRuntime = chatServerRuntime();
        if (chatServerRuntime != null) {
            chatServerRuntime.start(args);
            return;
        }
        ChatServer.start(args);
    }

    public void prepareShutdown() {
        processBridge().shutdownHook();
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

    private synchronized ChatProcessRuntimeBridge processBridge() {
        if (processBridge == null) {
            if (processBridgeProvider == null) {
                processBridge = new ChatProcessRuntimeBridge();
            } else {
                processBridge = processBridgeProvider.getIfAvailable(ChatProcessRuntimeBridge::new);
            }
        }
        return processBridge;
    }
}
