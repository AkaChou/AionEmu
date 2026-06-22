package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class ChatServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;

    public ChatServiceLifecycle(AionServicesProperties services) {
        this.services = services;
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public int getPhase() {
        return 200;
    }

    @Override
    public boolean isEnabled() {
        return services.getChat().isEnabled();
    }

    @Override
    public void start(ApplicationArguments args) {
        com.aionemu.chatserver.ChatServer.start(args.getSourceArgs());
    }
}
