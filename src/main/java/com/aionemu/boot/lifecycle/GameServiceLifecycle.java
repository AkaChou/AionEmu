package com.aionemu.boot.lifecycle;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.gameserver.lifecycle.GameBattlefieldLifecycle;
import com.aionemu.gameserver.lifecycle.GameCleaningLifecycle;
import com.aionemu.gameserver.lifecycle.GameCustomEventsLifecycle;
import com.aionemu.gameserver.lifecycle.GameDisputeLandLifecycle;
import com.aionemu.gameserver.lifecycle.GameDredgionLifecycle;
import com.aionemu.gameserver.lifecycle.GameEnginesLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeLifecycle;
import com.aionemu.gameserver.lifecycle.GameGeoNavLifecycle;
import com.aionemu.gameserver.lifecycle.GameHtmlLifecycle;
import com.aionemu.gameserver.lifecycle.GameHousingLifecycle;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorLifecycle;
import com.aionemu.gameserver.lifecycle.GameRewardServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingLifecycle;
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
        GameDredgionLifecycle dredgionLifecycle,
        GameBattlefieldLifecycle battlefieldLifecycle,
        GameProtectorConquerorLifecycle protectorConquerorLifecycle,
        GameDisputeLandLifecycle disputeLandLifecycle,
        GameHtmlLifecycle htmlLifecycle,
        GameRewardServicesLifecycle rewardServicesLifecycle,
        GameRuntimeServicesLifecycle runtimeServicesLifecycle,
        GameOptionalServicesLifecycle optionalServicesLifecycle,
        GameSeasonRankingLifecycle seasonRankingLifecycle,
        GameHousingLifecycle housingLifecycle,
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
            dredgionLifecycle,
            battlefieldLifecycle,
            protectorConquerorLifecycle,
            disputeLandLifecycle,
            htmlLifecycle,
            rewardServicesLifecycle,
            runtimeServicesLifecycle,
            optionalServicesLifecycle,
            seasonRankingLifecycle,
            housingLifecycle,
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
                siegeScheduleLifecycle,
                dredgionLifecycle,
                battlefieldLifecycle,
                protectorConquerorLifecycle,
                disputeLandLifecycle,
                htmlLifecycle,
                rewardServicesLifecycle,
                runtimeServicesLifecycle,
                optionalServicesLifecycle,
                seasonRankingLifecycle,
                housingLifecycle
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
        GameDredgionLifecycle dredgionLifecycle,
        GameBattlefieldLifecycle battlefieldLifecycle,
        GameProtectorConquerorLifecycle protectorConquerorLifecycle,
        GameDisputeLandLifecycle disputeLandLifecycle,
        GameHtmlLifecycle htmlLifecycle,
        GameRewardServicesLifecycle rewardServicesLifecycle,
        GameRuntimeServicesLifecycle runtimeServicesLifecycle,
        GameOptionalServicesLifecycle optionalServicesLifecycle,
        GameSeasonRankingLifecycle seasonRankingLifecycle,
        GameHousingLifecycle housingLifecycle,
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
