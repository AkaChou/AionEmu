package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.StandardEnvironment;

class GameServiceLifecycleTest {

    @Test
    void startAppliesLegacyConfigOverridesBeforeStartingGameServer() {
        AionServicesProperties services = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        LegacyConfigOverrides overrides = new RecordingLegacyConfigOverrides(events);
        GameStaticDataLifecycle staticDataLifecycle = new RecordingGameStaticDataLifecycle(events);
        GameWorldBootstrapLifecycle worldBootstrapLifecycle = new RecordingGameWorldBootstrapLifecycle(events);
        GameEventBootstrapLifecycle eventBootstrapLifecycle = new RecordingGameEventBootstrapLifecycle(events);
        GameGeoNavLifecycle geoNavLifecycle = new RecordingGameGeoNavLifecycle(events);
        GameWorldActivationLifecycle worldActivationLifecycle = new RecordingGameWorldActivationLifecycle(events);
        GameEnginesLifecycle enginesLifecycle = new RecordingGameEnginesLifecycle(events);
        GameLocationBootstrapLifecycle locationBootstrapLifecycle = new RecordingGameLocationBootstrapLifecycle(events);
        GameSpawnLifecycle spawnLifecycle = new RecordingGameSpawnLifecycle(events);
        GameEventRuntimeLifecycle eventRuntimeLifecycle = new RecordingGameEventRuntimeLifecycle(events);
        GameCleaningLifecycle cleaningLifecycle = new RecordingGameCleaningLifecycle(events);
        GameScheduledServicesLifecycle scheduledServicesLifecycle = new RecordingGameScheduledServicesLifecycle(events);
        GameCustomEventsLifecycle customEventsLifecycle = new RecordingGameCustomEventsLifecycle(events);
        GameSiegeScheduleLifecycle siegeScheduleLifecycle = new RecordingGameSiegeScheduleLifecycle(events);
        GameDredgionLifecycle dredgionLifecycle = new RecordingGameDredgionLifecycle(events);
        GameBattlefieldLifecycle battlefieldLifecycle = new RecordingGameBattlefieldLifecycle(events);
        GameProtectorConquerorLifecycle protectorConquerorLifecycle = new RecordingGameProtectorConquerorLifecycle(events);
        GameDisputeLandLifecycle disputeLandLifecycle = new RecordingGameDisputeLandLifecycle(events);
        GameHtmlLifecycle htmlLifecycle = new RecordingGameHtmlLifecycle(events);
        GameRewardServicesLifecycle rewardServicesLifecycle = new RecordingGameRewardServicesLifecycle(events);
        GameRuntimeServicesLifecycle runtimeServicesLifecycle = new RecordingGameRuntimeServicesLifecycle(events);
        GameOptionalServicesLifecycle optionalServicesLifecycle = new RecordingGameOptionalServicesLifecycle(events);
        GameSeasonRankingLifecycle seasonRankingLifecycle = new RecordingGameSeasonRankingLifecycle(events);
        GameThreadPoolLifecycle threadPoolLifecycle = new RecordingGameThreadPoolLifecycle(events);
        BiConsumer<String[], Boolean> startAction = (args, chatEnabled) -> events.add("start:" + chatEnabled);
        Runnable stopAction = () -> events.add("stop");
        GameServiceLifecycle lifecycle = new GameServiceLifecycle(
            services,
            overrides,
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
            threadPoolLifecycle,
            startAction,
            stopAction
        );

        lifecycle.start(new DefaultApplicationArguments("--example=true"));

        assertEquals(List.of("apply", "start:false"), events);
    }

    @Test
    void stopClosesThreadPoolAfterStoppingGameServer() {
        AionServicesProperties services = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        LegacyConfigOverrides overrides = new RecordingLegacyConfigOverrides(events);
        GameStaticDataLifecycle staticDataLifecycle = new RecordingGameStaticDataLifecycle(events);
        GameWorldBootstrapLifecycle worldBootstrapLifecycle = new RecordingGameWorldBootstrapLifecycle(events);
        GameEventBootstrapLifecycle eventBootstrapLifecycle = new RecordingGameEventBootstrapLifecycle(events);
        GameGeoNavLifecycle geoNavLifecycle = new RecordingGameGeoNavLifecycle(events);
        GameWorldActivationLifecycle worldActivationLifecycle = new RecordingGameWorldActivationLifecycle(events);
        GameEnginesLifecycle enginesLifecycle = new RecordingGameEnginesLifecycle(events);
        GameLocationBootstrapLifecycle locationBootstrapLifecycle = new RecordingGameLocationBootstrapLifecycle(events);
        GameSpawnLifecycle spawnLifecycle = new RecordingGameSpawnLifecycle(events);
        GameEventRuntimeLifecycle eventRuntimeLifecycle = new RecordingGameEventRuntimeLifecycle(events);
        GameCleaningLifecycle cleaningLifecycle = new RecordingGameCleaningLifecycle(events);
        GameScheduledServicesLifecycle scheduledServicesLifecycle = new RecordingGameScheduledServicesLifecycle(events);
        GameCustomEventsLifecycle customEventsLifecycle = new RecordingGameCustomEventsLifecycle(events);
        GameSiegeScheduleLifecycle siegeScheduleLifecycle = new RecordingGameSiegeScheduleLifecycle(events);
        GameDredgionLifecycle dredgionLifecycle = new RecordingGameDredgionLifecycle(events);
        GameBattlefieldLifecycle battlefieldLifecycle = new RecordingGameBattlefieldLifecycle(events);
        GameProtectorConquerorLifecycle protectorConquerorLifecycle = new RecordingGameProtectorConquerorLifecycle(events);
        GameDisputeLandLifecycle disputeLandLifecycle = new RecordingGameDisputeLandLifecycle(events);
        GameHtmlLifecycle htmlLifecycle = new RecordingGameHtmlLifecycle(events);
        GameRewardServicesLifecycle rewardServicesLifecycle = new RecordingGameRewardServicesLifecycle(events);
        GameRuntimeServicesLifecycle runtimeServicesLifecycle = new RecordingGameRuntimeServicesLifecycle(events);
        GameOptionalServicesLifecycle optionalServicesLifecycle = new RecordingGameOptionalServicesLifecycle(events);
        GameSeasonRankingLifecycle seasonRankingLifecycle = new RecordingGameSeasonRankingLifecycle(events);
        GameThreadPoolLifecycle threadPoolLifecycle = new RecordingGameThreadPoolLifecycle(events);
        GameServiceLifecycle lifecycle = new GameServiceLifecycle(
            services,
            overrides,
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
            threadPoolLifecycle,
            (args, chatEnabled) -> { },
            () -> events.add("stop")
        );

        threadPoolLifecycle.start();
        lifecycle.stop();

        assertEquals(List.of("threadPool:start", "stop", "threadPool:stop"), events);
    }

    private static final class RecordingLegacyConfigOverrides extends LegacyConfigOverrides {

        private final List<String> events;

        private RecordingLegacyConfigOverrides(List<String> events) {
            super(new StandardEnvironment());
            this.events = events;
        }

        @Override
        public void applyToGameConfig() {
            events.add("apply");
        }
    }

    private static final class RecordingGameStaticDataLifecycle extends GameStaticDataLifecycle {

        private final List<String> events;

        private RecordingGameStaticDataLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("staticData:start");
        }
    }

    private static final class RecordingGameWorldBootstrapLifecycle extends GameWorldBootstrapLifecycle {

        private final List<String> events;

        private RecordingGameWorldBootstrapLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("worldBootstrap:start");
        }
    }

    private static final class RecordingGameEventBootstrapLifecycle extends GameEventBootstrapLifecycle {

        private final List<String> events;

        private RecordingGameEventBootstrapLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("eventBootstrap:start");
        }
    }

    private static final class RecordingGameGeoNavLifecycle extends GameGeoNavLifecycle {

        private final List<String> events;

        private RecordingGameGeoNavLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("geoNav:start");
        }
    }

    private static final class RecordingGameWorldActivationLifecycle extends GameWorldActivationLifecycle {

        private final List<String> events;

        private RecordingGameWorldActivationLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start(Runnable activeServerSetter) {
            events.add("worldActivation:start");
        }
    }

    private static final class RecordingGameEnginesLifecycle extends GameEnginesLifecycle {

        private final List<String> events;

        private RecordingGameEnginesLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("engines:start");
        }
    }

    private static final class RecordingGameLocationBootstrapLifecycle extends GameLocationBootstrapLifecycle {

        private final List<String> events;

        private RecordingGameLocationBootstrapLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("locationBootstrap:start");
        }
    }

    private static final class RecordingGameSpawnLifecycle extends GameSpawnLifecycle {

        private final List<String> events;

        private RecordingGameSpawnLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("spawn:start");
        }
    }

    private static final class RecordingGameEventRuntimeLifecycle extends GameEventRuntimeLifecycle {

        private final List<String> events;

        private RecordingGameEventRuntimeLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("eventRuntime:start");
        }
    }

    private static final class RecordingGameCleaningLifecycle extends GameCleaningLifecycle {

        private final List<String> events;

        private RecordingGameCleaningLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("cleaning:start");
        }
    }

    private static final class RecordingGameScheduledServicesLifecycle extends GameScheduledServicesLifecycle {

        private final List<String> events;

        private RecordingGameScheduledServicesLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("scheduledServices:start");
        }
    }

    private static final class RecordingGameCustomEventsLifecycle extends GameCustomEventsLifecycle {

        private final List<String> events;

        private RecordingGameCustomEventsLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("customEvents:start");
        }
    }

    private static final class RecordingGameSiegeScheduleLifecycle extends GameSiegeScheduleLifecycle {

        private final List<String> events;

        private RecordingGameSiegeScheduleLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("siegeSchedule:start");
        }
    }

    private static final class RecordingGameDredgionLifecycle extends GameDredgionLifecycle {

        private final List<String> events;

        private RecordingGameDredgionLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("dredgion:start");
        }
    }

    private static final class RecordingGameBattlefieldLifecycle extends GameBattlefieldLifecycle {

        private final List<String> events;

        private RecordingGameBattlefieldLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("battlefield:start");
        }
    }

    private static final class RecordingGameProtectorConquerorLifecycle extends GameProtectorConquerorLifecycle {

        private final List<String> events;

        private RecordingGameProtectorConquerorLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("protectorConqueror:start");
        }
    }

    private static final class RecordingGameDisputeLandLifecycle extends GameDisputeLandLifecycle {

        private final List<String> events;

        private RecordingGameDisputeLandLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("disputeLand:start");
        }
    }

    private static final class RecordingGameHtmlLifecycle extends GameHtmlLifecycle {

        private final List<String> events;

        private RecordingGameHtmlLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("html:start");
        }
    }

    private static final class RecordingGameRewardServicesLifecycle extends GameRewardServicesLifecycle {

        private final List<String> events;

        private RecordingGameRewardServicesLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("rewardServices:start");
        }
    }

    private static final class RecordingGameRuntimeServicesLifecycle extends GameRuntimeServicesLifecycle {

        private final List<String> events;

        private RecordingGameRuntimeServicesLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("runtimeServices:start");
        }
    }

    private static final class RecordingGameOptionalServicesLifecycle extends GameOptionalServicesLifecycle {

        private final List<String> events;

        private RecordingGameOptionalServicesLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("optionalServices:start");
        }
    }

    private static final class RecordingGameSeasonRankingLifecycle extends GameSeasonRankingLifecycle {

        private final List<String> events;

        private RecordingGameSeasonRankingLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("seasonRanking:start");
        }
    }

    private static final class RecordingGameThreadPoolLifecycle extends GameThreadPoolLifecycle {

        private final List<String> events;
        private boolean started;

        private RecordingGameThreadPoolLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            if (started) {
                return;
            }
            events.add("threadPool:start");
            started = true;
        }

        @Override
        public synchronized void stop() {
            if (!started) {
                return;
            }
            events.add("threadPool:stop");
            started = false;
        }
    }
}
