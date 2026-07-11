package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

/**
 * 内嵌游戏服生命周期：配置路径、应用遗留覆盖并驱动启动序列与网络/线程池关闭。
 * Embedded game-service lifecycle: configures paths, applies legacy overrides, and drives startup and network/thread-pool stop.
 */
@Component
@RequiredArgsConstructor
public class GameServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyConfigOverrides legacyConfigOverrides;
    private final GameStartupSequenceLifecycle startupSequenceLifecycle;
    private final GameServerNetworkLifecycle serverNetworkLifecycle;
    private final GameThreadPoolLifecycle threadPoolLifecycle;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "game";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPhase() {
        return 300;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return services.getGame().isEnabled();
    }

    /**
     * 配置游戏路径与遗留配置，并启动游戏启动序列。
     * Configures game paths and legacy settings, then starts the game startup sequence.
     *
     * @param args 应用启动参数 / application arguments
     */
    @Override
    public void start(ApplicationArguments args) {
        AionServicePaths.configureGame();
        legacyConfigOverrides.applyToGameConfig();
        startupSequenceLifecycle.start(services.getChat().isEnabled());
    }

    /**
     * 先停网络再停线程池。
     * Stops the network first, then the thread pool.
     */
    @Override
    public void stop() {
        try {
            serverNetworkLifecycle.stop();
        } finally {
            threadPoolLifecycle.stop();
        }
    }
}
