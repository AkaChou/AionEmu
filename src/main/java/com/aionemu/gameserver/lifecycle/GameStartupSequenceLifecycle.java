package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameStartupSequenceLifecycle {

    private final GameThreadPoolLifecycle threadPoolLifecycle;
    private final GameStaticDataLifecycle staticDataLifecycle;
    private final GameWorldBootstrapLifecycle worldBootstrapLifecycle;
    private final GameEventBootstrapLifecycle eventBootstrapLifecycle;
    private final GameGeoNavLifecycle geoNavLifecycle;
    private final GameWorldActivationLifecycle worldActivationLifecycle;
    private final GameEnginesLifecycle enginesLifecycle;
    private final GameLocationBootstrapLifecycle locationBootstrapLifecycle;
    private final GameSpawnLifecycle spawnLifecycle;
    private final GameEventRuntimeLifecycle eventRuntimeLifecycle;
    private final GameCleaningLifecycle cleaningLifecycle;
    private final GameScheduledServicesLifecycle scheduledServicesLifecycle;
    private final GameCustomEventsLifecycle customEventsLifecycle;
    private final GameSiegeScheduleLifecycle siegeScheduleLifecycle;
    private final GameDredgionLifecycle dredgionLifecycle;
    private final GameBattlefieldLifecycle battlefieldLifecycle;
    private final GameProtectorConquerorLifecycle protectorConquerorLifecycle;
    private final GameDisputeLandLifecycle disputeLandLifecycle;
    private final GameHtmlLifecycle htmlLifecycle;
    private final GameRewardServicesLifecycle rewardServicesLifecycle;
    private final GameRuntimeServicesLifecycle runtimeServicesLifecycle;
    private final GameOptionalServicesLifecycle optionalServicesLifecycle;
    private final GameSeasonRankingLifecycle seasonRankingLifecycle;
    private final GameHousingLifecycle housingLifecycle;
    private final GameSystemLifecycle systemLifecycle;
    private final GameServerNetworkLifecycle serverNetworkLifecycle;
    private final GameNetworkStartupLifecycle networkStartupLifecycle;
    private final GameRatioLimitLifecycle ratioLimitLifecycle;
    private final GameStartupHooksLifecycle startupHooksLifecycle;
    private final GameStartupCompletionLifecycle startupCompletionLifecycle;
    private final GameLoggingLifecycle loggingLifecycle;
    private final GameUtilityServicesLifecycle utilityServicesLifecycle;
    private final GameAdminPanelLifecycle adminPanelLifecycle;
    private final GameSystemPropertiesLifecycle systemPropertiesLifecycle;
    private final GameStartupLogLifecycle startupLogLifecycle;
    private final GameChatServerOverrideLifecycle chatServerOverrideLifecycle;

    public void start(Boolean chatServerEnabledOverride) {
        systemPropertiesLifecycle.start();
        long start = startupLogLifecycle.start();

        loggingLifecycle.start();
        utilityServicesLifecycle.start(threadPoolLifecycle);
        chatServerOverrideLifecycle.start(chatServerEnabledOverride);
        adminPanelLifecycle.start();

        staticDataLifecycle.start();
        worldBootstrapLifecycle.start();
        eventBootstrapLifecycle.start();

        geoNavLifecycle.start();
        GameServer gs = worldActivationLifecycle.start();

        enginesLifecycle.start();
        locationBootstrapLifecycle.start();
        spawnLifecycle.start();
        eventRuntimeLifecycle.start();
        cleaningLifecycle.start();
        scheduledServicesLifecycle.start();
        customEventsLifecycle.start();
        siegeScheduleLifecycle.start();
        dredgionLifecycle.start();
        battlefieldLifecycle.start();
        protectorConquerorLifecycle.start();
        disputeLandLifecycle.start();
        htmlLifecycle.start();
        rewardServicesLifecycle.start();
        runtimeServicesLifecycle.start();
        optionalServicesLifecycle.start();
        seasonRankingLifecycle.start();
        housingLifecycle.start();

        long startupTime = systemLifecycle.start(start);

        networkStartupLifecycle.start(() -> serverNetworkLifecycle.start(gs));
        ratioLimitLifecycle.start();
        startupHooksLifecycle.start();
        startupCompletionLifecycle.start(startupTime);
    }
}
