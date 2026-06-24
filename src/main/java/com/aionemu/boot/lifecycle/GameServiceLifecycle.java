package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyConfigOverrides legacyConfigOverrides;
    private final GameStartupSequenceLifecycle startupSequenceLifecycle;
    private final GameServerNetworkLifecycle serverNetworkLifecycle;
    private final GameThreadPoolLifecycle threadPoolLifecycle;

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public int getPhase() {
        return 300;
    }

    @Override
    public boolean isEnabled() {
        return services.getGame().isEnabled();
    }

    @Override
    public void start(ApplicationArguments args) {
        AionServicePaths.configureGame();
        legacyConfigOverrides.applyToGameConfig();
        startupSequenceLifecycle.start(services.getChat().isEnabled());
    }

    @Override
    public void stop() {
        try {
            serverNetworkLifecycle.stop();
        } finally {
            threadPoolLifecycle.stop();
        }
    }
}
