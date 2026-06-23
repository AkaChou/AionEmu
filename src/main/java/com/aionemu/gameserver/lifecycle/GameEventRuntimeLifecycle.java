package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.configs.main.RankingConfig;
import com.aionemu.gameserver.services.EventService;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.spawnengine.TemporarySpawnEngine;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameEventRuntimeLifecycle {

    private final BooleanSupplier eventServiceEnabled;
    private final Runnable eventServiceStarter;
    private final BooleanSupplier playerEventEnabled;
    private final Runnable playerEventInitializer;
    private final BooleanSupplier crazyEventEnabled;
    private final Runnable crazyEventTimerStarter;
    private final BooleanSupplier topRankingUpdateEnabled;
    private final Runnable rankingHourlyScheduler;
    private final Runnable rankingMinuteScheduler;
    private final Runnable rewardWeeklyInitializer;
    private final Runnable packetBroadcasterInitializer;
    private final Runnable temporarySpawner;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameEventRuntimeLifecycle() {
        this(
            () -> EventsConfig.ENABLE_EVENT_SERVICE,
            () -> EventService.getInstance().start(),
            () -> EventsConfig.EVENT_ENABLED,
            PlayerEventService::getInstance,
            () -> EventsConfig.ENABLE_CRAZY,
            () -> CrazyDaevaService.getInstance().startTimer(),
            () -> RankingConfig.TOP_RANKING_UPDATE_SETTING,
            () -> AbyssRankUpdateService.getInstance().scheduleUpdateHour(),
            () -> AbyssRankUpdateService.getInstance().scheduleUpdateMinute(),
            () -> AbyssRankUpdateService.getInstance().initRewardWeeklyManager(),
            PacketBroadcaster::getInstance,
            TemporarySpawnEngine::spawnAll
        );
    }

    GameEventRuntimeLifecycle(
        BooleanSupplier eventServiceEnabled,
        Runnable eventServiceStarter,
        BooleanSupplier playerEventEnabled,
        Runnable playerEventInitializer,
        BooleanSupplier crazyEventEnabled,
        Runnable crazyEventTimerStarter,
        BooleanSupplier topRankingUpdateEnabled,
        Runnable rankingHourlyScheduler,
        Runnable rankingMinuteScheduler,
        Runnable rewardWeeklyInitializer,
        Runnable packetBroadcasterInitializer,
        Runnable temporarySpawner
    ) {
        this.eventServiceEnabled = eventServiceEnabled;
        this.eventServiceStarter = eventServiceStarter;
        this.playerEventEnabled = playerEventEnabled;
        this.playerEventInitializer = playerEventInitializer;
        this.crazyEventEnabled = crazyEventEnabled;
        this.crazyEventTimerStarter = crazyEventTimerStarter;
        this.topRankingUpdateEnabled = topRankingUpdateEnabled;
        this.rankingHourlyScheduler = rankingHourlyScheduler;
        this.rankingMinuteScheduler = rankingMinuteScheduler;
        this.rewardWeeklyInitializer = rewardWeeklyInitializer;
        this.packetBroadcasterInitializer = packetBroadcasterInitializer;
        this.temporarySpawner = temporarySpawner;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (eventServiceEnabled.getAsBoolean()) {
                eventServiceStarter.run();
            }
            if (playerEventEnabled.getAsBoolean()) {
                playerEventInitializer.run();
            }
            if (crazyEventEnabled.getAsBoolean()) {
                crazyEventTimerStarter.run();
            }
            if (topRankingUpdateEnabled.getAsBoolean()) {
                rankingHourlyScheduler.run();
            } else {
                rankingMinuteScheduler.run();
            }
            rewardWeeklyInitializer.run();
            packetBroadcasterInitializer.run();
            temporarySpawner.run();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
