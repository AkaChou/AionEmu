package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class GameServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyConfigOverrides legacyConfigOverrides;
    private final GameThreadPoolLifecycle threadPoolLifecycle;
    private final Consumer<Boolean> startAction;
    private final Runnable stopAction;

    @Autowired
    public GameServiceLifecycle(
        AionServicesProperties services,
        LegacyConfigOverrides legacyConfigOverrides,
        GameStartupSequenceLifecycle startupSequenceLifecycle,
        GameThreadPoolLifecycle threadPoolLifecycle
    ) {
        this(
            services,
            legacyConfigOverrides,
            threadPoolLifecycle,
            startupSequenceLifecycle::start,
            com.aionemu.gameserver.GameServer::stop
        );
    }

    GameServiceLifecycle(
        AionServicesProperties services,
        LegacyConfigOverrides legacyConfigOverrides,
        GameThreadPoolLifecycle threadPoolLifecycle,
        Consumer<Boolean> startAction,
        Runnable stopAction
    ) {
        this.services = services;
        this.legacyConfigOverrides = legacyConfigOverrides;
        this.threadPoolLifecycle = threadPoolLifecycle;
        this.startAction = startAction;
        this.stopAction = stopAction;
    }

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
        startAction.accept(services.getChat().isEnabled());
    }

    @Override
    public void stop() {
        try {
            stopAction.run();
        } finally {
            threadPoolLifecycle.stop();
        }
    }
}
