package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServerRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatServerLifecycleGateway {

    private ChatServerRuntime chatServerRuntime;

    public ChatServerLifecycleGateway() {
    }

    @Autowired(required = false)
    void setChatServerRuntime(ChatServerRuntime chatServerRuntime) {
        this.chatServerRuntime = chatServerRuntime;
    }

    public void start(String[] args) {
        if (chatServerRuntime == null) {
            com.aionemu.chatserver.ChatServer.start(args);
            return;
        }
        chatServerRuntime.start(args);
    }

    public void stop() {
        com.aionemu.chatserver.ShutdownHook.getInstance().shutdown(false);
    }
}
