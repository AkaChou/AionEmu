package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class ChatServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private boolean started;

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
        AionServicePaths.configureChat();
        com.aionemu.chatserver.ChatServer.start(args.getSourceArgs());
        started = true;
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        com.aionemu.chatserver.network.netty.NettyServer.getInstance().shutdownAll();
        started = false;
    }
}
