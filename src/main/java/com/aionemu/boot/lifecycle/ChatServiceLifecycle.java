package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyChatConfigOverrides;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

/**
 * 内嵌聊天服生命周期：配置路径、应用遗留覆盖并委托网关启停。
 * Embedded chat-service lifecycle: configures paths, applies legacy overrides, and delegates start/stop.
 */
@Component
@RequiredArgsConstructor
public class ChatServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyChatConfigOverrides legacyConfigOverrides;
    private final ChatServerLifecycleGateway chatServerLifecycleGateway;
    private boolean started;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "chat";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPhase() {
        return 200;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return services.getChat().isEnabled();
    }

    /**
     * 配置聊天路径与遗留配置后启动；失败时立即 stop 清理。
     * Configures chat paths and legacy settings, then starts; stops immediately on failure.
     *
     * @param args 应用启动参数 / application arguments
     */
    @Override
    public void start(ApplicationArguments args) {
        AionServicePaths.configureChat();
        try {
            legacyConfigOverrides.applyToChatConfig();
            chatServerLifecycleGateway.start(args.getSourceArgs());
            started = true;
        } catch (RuntimeException | Error e) {
            chatServerLifecycleGateway.stop();
            throw e;
        }
    }

    /**
     * 若已成功启动则停止聊天服。
     * Stops the chat server when it was successfully started.
     */
    @Override
    public void stop() {
        if (!started) {
            return;
        }
        chatServerLifecycleGateway.stop();
        started = false;
    }
}
