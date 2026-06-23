package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final ChatServerLifecycleGateway chatServerLifecycleGateway;
    private boolean started;

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
            chatServerLifecycleGateway.start(args.getSourceArgs());
            started = true;
        } catch (RuntimeException | Error e) {
            chatServerLifecycleGateway.stop();
            throw e;
        }
    }

    @Override
    public void stop() {
        if (!started) {
            return;
        }
        chatServerLifecycleGateway.stop();
        started = false;
    }
}
