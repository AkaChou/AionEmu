package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class ChatServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final Consumer<String[]> startAction;
    private final Runnable stopAction;
    private boolean started;

    @Autowired
    public ChatServiceLifecycle(AionServicesProperties services) {
        this(
            services,
            com.aionemu.chatserver.ChatServer::start,
            () -> com.aionemu.chatserver.ShutdownHook.getInstance().shutdown(false)
        );
    }

    ChatServiceLifecycle(AionServicesProperties services, Consumer<String[]> startAction, Runnable stopAction) {
        this.services = services;
        this.startAction = startAction;
        this.stopAction = stopAction;
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
        try {
            startAction.accept(args.getSourceArgs());
            started = true;
        } catch (RuntimeException | Error e) {
            stopAction.run();
            throw e;
        }
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        stopAction.run();
        started = false;
    }
}
