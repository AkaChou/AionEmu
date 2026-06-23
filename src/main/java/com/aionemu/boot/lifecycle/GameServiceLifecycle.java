package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.gameserver.lifecycle.GameCleaningLifecycle;
import com.aionemu.gameserver.lifecycle.GameCustomEventsLifecycle;
import com.aionemu.gameserver.lifecycle.GameEnginesLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeLifecycle;
import com.aionemu.gameserver.lifecycle.GameGeoNavLifecycle;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleLifecycle;
import com.aionemu.gameserver.lifecycle.GameSpawnLifecycle;
import com.aionemu.gameserver.lifecycle.GameStaticDataLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldActivationLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapLifecycle;
import java.util.function.BiConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class GameServiceLifecycle implements AionServiceLifecycle {

    private final AionServicesProperties services;
    private final LegacyConfigOverrides legacyConfigOverrides;
    private final GameThreadPoolLifecycle threadPoolLifecycle;
    private final BiConsumer<String[], Boolean> startAction;
    private final Runnable stopAction;

    @Autowired
    public GameServiceLifecycle(
        AionServicesProperties services,
        LegacyConfigOverrides legacyConfigOverrides,
        GameStaticDataLifecycle staticDataLifecycle,
        GameWorldBootstrapLifecycle worldBootstrapLifecycle,
        GameEventBootstrapLifecycle eventBootstrapLifecycle,
        GameGeoNavLifecycle geoNavLifecycle,
        GameWorldActivationLifecycle worldActivationLifecycle,
        GameEnginesLifecycle enginesLifecycle,
        GameLocationBootstrapLifecycle locationBootstrapLifecycle,
        GameSpawnLifecycle spawnLifecycle,
        GameEventRuntimeLifecycle eventRuntimeLifecycle,
        GameCleaningLifecycle cleaningLifecycle,
        GameScheduledServicesLifecycle scheduledServicesLifecycle,
        GameCustomEventsLifecycle customEventsLifecycle,
        GameSiegeScheduleLifecycle siegeScheduleLifecycle,
        GameThreadPoolLifecycle threadPoolLifecycle
    ) {
        this(
            services,
            legacyConfigOverrides,
            staticDataLifecycle,
            worldBootstrapLifecycle,
            eventBootstrapLifecycle,
            geoNavLifecycle,
            worldActivationLifecycle,
            enginesLifecycle,
            locationBootstrapLifecycle,
            spawnLifecycle,
            eventRuntimeLifecycle,
            cleaningLifecycle,
            scheduledServicesLifecycle,
            customEventsLifecycle,
            siegeScheduleLifecycle,
            threadPoolLifecycle,
            (args, chatEnabled) -> com.aionemu.gameserver.GameServer.start(
                args,
                chatEnabled,
                threadPoolLifecycle,
                staticDataLifecycle,
                worldBootstrapLifecycle,
                eventBootstrapLifecycle,
                geoNavLifecycle,
                worldActivationLifecycle,
                enginesLifecycle,
                locationBootstrapLifecycle,
                spawnLifecycle,
                eventRuntimeLifecycle,
                cleaningLifecycle,
                scheduledServicesLifecycle,
                customEventsLifecycle,
                siegeScheduleLifecycle
            ),
            com.aionemu.gameserver.GameServer::stop
        );
    }

    GameServiceLifecycle(
        AionServicesProperties services,
        LegacyConfigOverrides legacyConfigOverrides,
        GameStaticDataLifecycle staticDataLifecycle,
        GameWorldBootstrapLifecycle worldBootstrapLifecycle,
        GameEventBootstrapLifecycle eventBootstrapLifecycle,
        GameGeoNavLifecycle geoNavLifecycle,
        GameWorldActivationLifecycle worldActivationLifecycle,
        GameEnginesLifecycle enginesLifecycle,
        GameLocationBootstrapLifecycle locationBootstrapLifecycle,
        GameSpawnLifecycle spawnLifecycle,
        GameEventRuntimeLifecycle eventRuntimeLifecycle,
        GameCleaningLifecycle cleaningLifecycle,
        GameScheduledServicesLifecycle scheduledServicesLifecycle,
        GameCustomEventsLifecycle customEventsLifecycle,
        GameSiegeScheduleLifecycle siegeScheduleLifecycle,
        GameThreadPoolLifecycle threadPoolLifecycle,
        BiConsumer<String[], Boolean> startAction,
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
        startAction.accept(args.getSourceArgs(), services.getChat().isEnabled());
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
