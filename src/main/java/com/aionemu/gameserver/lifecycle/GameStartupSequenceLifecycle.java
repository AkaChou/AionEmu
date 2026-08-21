package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游戏服启动总编排器：按固定阶段顺序驱动各 lifecycle（系统属性、日志、静态数据、
 * 世界/活动/引擎、刷怪、定时与功能服务、网络与启动钩子），最后汇总打印各阶段耗时。
 * Master startup orchestrator: drives lifecycle phases in a fixed order (system properties,
 * logging, static data, world/event/engines, spawns, scheduled/feature services, network and
 * startup hooks), then prints aggregated phase timings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameStartupSequenceLifecycle {

    /**
     * 线程池生命周期。
     * Thread-pool lifecycle.
     */
    private final GameThreadPoolLifecycle threadPoolLifecycle;

    /**
     * 静态数据生命周期。
     * Static-data lifecycle.
     */
    private final GameStaticDataLifecycle staticDataLifecycle;

    /**
     * 世界引导生命周期。
     * World-bootstrap lifecycle.
     */
    private final GameWorldBootstrapLifecycle worldBootstrapLifecycle;

    /**
     * 活动引导生命周期。
     * Event-bootstrap lifecycle.
     */
    private final GameEventBootstrapLifecycle eventBootstrapLifecycle;

    /**
     * 地理/PATH 生命周期。
     * Geo/path lifecycle.
     */
    private final GameGeoPathLifecycle geoPathLifecycle;

    /**
     * 世界激活生命周期。
     * World-activation lifecycle.
     */
    private final GameWorldActivationLifecycle worldActivationLifecycle;

    /**
     * 引擎生命周期。
     * Engines lifecycle.
     */
    private final GameEnginesLifecycle enginesLifecycle;

    /**
     * 地点引导生命周期。
     * Location-bootstrap lifecycle.
     */
    private final GameLocationBootstrapLifecycle locationBootstrapLifecycle;

    /**
     * 刷怪生命周期。
     * Spawn lifecycle.
     */
    private final GameSpawnLifecycle spawnLifecycle;

    /**
     * 活动运行时生命周期。
     * Event-runtime lifecycle.
     */
    private final GameEventRuntimeLifecycle eventRuntimeLifecycle;

    /**
     * 清理生命周期。
     * Cleaning lifecycle.
     */
    private final GameCleaningLifecycle cleaningLifecycle;

    /**
     * 定时服务生命周期。
     * Scheduled-services lifecycle.
     */
    private final GameScheduledServicesLifecycle scheduledServicesLifecycle;

    /**
     * 自定义活动生命周期。
     * Custom-events lifecycle.
     */
    private final GameCustomEventsLifecycle customEventsLifecycle;

    /**
     * 攻城日程生命周期。
     * Siege-schedule lifecycle.
     */
    private final GameSiegeScheduleLifecycle siegeScheduleLifecycle;

    /**
     * 欧比斯/无畏舰生命周期。
     * Dredgion lifecycle.
     */
    private final GameDredgionLifecycle dredgionLifecycle;

    /**
     * 战场生命周期。
     * Battlefield lifecycle.
     */
    private final GameBattlefieldLifecycle battlefieldLifecycle;

    /**
     * 守护者/征服者生命周期。
     * Protector/conqueror lifecycle.
     */
    private final GameProtectorConquerorLifecycle protectorConquerorLifecycle;

    /**
     * 争议领地生命周期。
     * Dispute-land lifecycle.
     */
    private final GameDisputeLandLifecycle disputeLandLifecycle;

    /**
     * HTML 生命周期。
     * HTML lifecycle.
     */
    private final GameHtmlLifecycle htmlLifecycle;

    /**
     * 奖励服务生命周期。
     * Reward-services lifecycle.
     */
    private final GameRewardServicesLifecycle rewardServicesLifecycle;

    /**
     * 运行时服务集合生命周期。
     * Runtime-services lifecycle.
     */
    private final GameRuntimeServicesLifecycle runtimeServicesLifecycle;

    /**
     * 可选服务生命周期。
     * Optional-services lifecycle.
     */
    private final GameOptionalServicesLifecycle optionalServicesLifecycle;

    /**
     * 赛季排名生命周期。
     * Season-ranking lifecycle.
     */
    private final GameSeasonRankingLifecycle seasonRankingLifecycle;

    /**
     * 房屋生命周期。
     * Housing lifecycle.
     */
    private final GameHousingLifecycle housingLifecycle;

    /**
     * 系统生命周期。
     * System lifecycle.
     */
    private final GameSystemLifecycle systemLifecycle;

    /**
     * 服务器网络生命周期。
     * Server-network lifecycle.
     */
    private final GameServerNetworkLifecycle serverNetworkLifecycle;

    /**
     * 网络启动生命周期。
     * Network-startup lifecycle.
     */
    private final GameNetworkStartupLifecycle networkStartupLifecycle;

    /**
     * 阵营比例限制生命周期。
     * Ratio-limit lifecycle.
     */
    private final GameRatioLimitLifecycle ratioLimitLifecycle;

    /**
     * 启动钩子生命周期。
     * Startup-hooks lifecycle.
     */
    private final GameStartupHooksLifecycle startupHooksLifecycle;

    /**
     * 启动完成生命周期。
     * Startup-completion lifecycle.
     */
    private final GameStartupCompletionLifecycle startupCompletionLifecycle;

    /**
     * 日志生命周期。
     * Logging lifecycle.
     */
    private final GameLoggingLifecycle loggingLifecycle;

    /**
     * 工具服务生命周期。
     * Utility-services lifecycle.
     */
    private final GameUtilityServicesLifecycle utilityServicesLifecycle;

    /**
     * 管理面板生命周期。
     * Admin-panel lifecycle.
     */
    private final GameAdminPanelLifecycle adminPanelLifecycle;

    /**
     * 系统属性生命周期。
     * System-properties lifecycle.
     */
    private final GameSystemPropertiesLifecycle systemPropertiesLifecycle;

    /**
     * 启动日志计时生命周期。
     * Startup-log timing lifecycle.
     */
    private final GameStartupLogLifecycle startupLogLifecycle;

    /**
     * 聊天服覆盖生命周期。
     * Chat-server override lifecycle.
     */
    private final GameChatServerOverrideLifecycle chatServerOverrideLifecycle;

    /**
     * 按阶段顺序执行完整启动：预热 JAXB、系统属性与日志、静态数据与世界、
     * 引擎/刷怪/功能服务、网络与钩子，最后汇总耗时。
     * Run the full startup in phase order: JAXB warm-up, system properties and logging,
     * static data and world, engines/spawns/feature services, network and hooks, then timing summary.
     *
     * @param chatServerEnabledOverride 聊天服启用覆盖（可为 null 表示沿用配置） /
     *                                  chat-server enable override (null keeps config)
     */
    public void start(Boolean chatServerEnabledOverride) {
        XmlDataLoader.preloadContextAsync(); // 尽早异步预热 StaticData JAXBContext，与后续启动步骤并行（借鉴 aion-server GameServer:93）
        // Warm up the StaticData JAXBContext asynchronously as early as possible, in parallel with later startup steps (based on aion-server GameServer:93).
        systemPropertiesLifecycle.start();
        long start = startupLogLifecycle.start();

        loggingLifecycle.start();
        utilityServicesLifecycle.start(threadPoolLifecycle);
        chatServerOverrideLifecycle.start(chatServerEnabledOverride);
        adminPanelLifecycle.start();

        // Raw quest XML compilation has no static-data dependency; only NPC validation does.
        // Raw quest XML 编译不依赖静态数据；只有 NPC 校验依赖静态数据。
        enginesLifecycle.preloadProductionCatalog();
        staticDataLifecycle.start();
        geoPathLifecycle.start();
        worldBootstrapLifecycle.start();
        eventBootstrapLifecycle.start();

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
    // ponytail: each lifecycle records its own loadTimeMillis; reflectively aggregate them here once, new lifecycles are picked up automatically without a maintenance list
    /**
     * 反射读取各 lifecycle 字段的 {@code getLoadTimeMillis()}，按耗时降序打印阶段计时。
     * Reflectively read each lifecycle field's {@code getLoadTimeMillis()} and log timings descending.
     */
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
                // Skip non-lifecycle fields or those without a timing method.
            }
        }
        timings.sort(Map.Entry.<String, Long>comparingByValue().reversed());
        log.info(I18n.get("console.startup.phase_timings"));
        timings.forEach(e -> log.info(I18n.get("log.f50a88761c02", String.format("%-28s", e.getKey()), String.format("%7d", e.getValue()))));
    }
}
