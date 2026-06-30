package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.GameServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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
        logStartupPhaseTimings();
    }

    // ponytail: 每个 lifecycle 已在自身记录 loadTimeMillis，这里反射汇总打印一次；新增 lifecycle 自动纳入，无需维护列表
    private void logStartupPhaseTimings() {
        List<Map.Entry<String, Long>> timings = new ArrayList<>();
        for (Field f : getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object bean = f.get(this);
                long ms = (Long) bean.getClass().getMethod("getLoadTimeMillis").invoke(bean);
                if (ms >= 0) {
                    timings.add(new java.util.AbstractMap.SimpleEntry<>(f.getName(), ms));
                }
            } catch (ReflectiveOperationException ignored) {
                // 非 lifecycle 字段或无计时方法，跳过
            }
        }
        timings.sort(Map.Entry.<String, Long>comparingByValue().reversed());
        log.info("Startup phase timings (ms, slowest first):");
        timings.forEach(e -> log.info(String.format("  %-28s %7d", e.getKey(), e.getValue())));
    }
}
