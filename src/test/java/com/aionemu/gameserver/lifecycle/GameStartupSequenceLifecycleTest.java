package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.GameServer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStartupSequenceLifecycleTest {

    @Test
    void startRunsGameStartupStepsInOrder() {
        List<String> events = new ArrayList<>();
        GameStartupSequenceLifecycle lifecycle = new GameStartupSequenceLifecycle(
            new RecordingGameThreadPoolLifecycle(),
            new RecordingGameStaticDataLifecycle(events),
            new RecordingGameWorldBootstrapLifecycle(events),
            new RecordingGameEventBootstrapLifecycle(events),
            new RecordingGameGeoNavLifecycle(events),
            new RecordingGameWorldActivationLifecycle(events),
            new RecordingGameEnginesLifecycle(events),
            new RecordingGameLocationBootstrapLifecycle(events),
            new RecordingGameSpawnLifecycle(events),
            new RecordingGameEventRuntimeLifecycle(events),
            new RecordingGameCleaningLifecycle(events),
            new RecordingGameScheduledServicesLifecycle(events),
            new RecordingGameCustomEventsLifecycle(events),
            new RecordingGameSiegeScheduleLifecycle(events),
            new RecordingGameDredgionLifecycle(events),
            new RecordingGameBattlefieldLifecycle(events),
            new RecordingGameProtectorConquerorLifecycle(events),
            new RecordingGameDisputeLandLifecycle(events),
            new RecordingGameHtmlLifecycle(events),
            new RecordingGameRewardServicesLifecycle(events),
            new RecordingGameRuntimeServicesLifecycle(events),
            new RecordingGameOptionalServicesLifecycle(events),
            new RecordingGameSeasonRankingLifecycle(events),
            new RecordingGameHousingLifecycle(events),
            new RecordingGameSystemLifecycle(events),
            new RecordingGameServerNetworkLifecycle(events),
            new RecordingGameNetworkStartupLifecycle(events),
            new RecordingGameRatioLimitLifecycle(events),
            new RecordingGameStartupHooksLifecycle(events),
            new RecordingGameStartupCompletionLifecycle(events),
            new RecordingGameLoggingLifecycle(events),
            new RecordingGameUtilityServicesLifecycle(events),
            new RecordingGameAdminPanelLifecycle(events),
            new RecordingGameSystemPropertiesLifecycle(events),
            new RecordingGameStartupLogLifecycle(events),
            new RecordingGameChatServerOverrideLifecycle(events)
        );

        lifecycle.start(true);

        assertEquals(List.of(
            "systemProperties:start",
            "startupLog:start",
            "logging:start",
            "utilityServices:start",
            "chatOverride:start:true",
            "adminPanel:start",
            "staticData:start",
            "worldBootstrap:start",
            "eventBootstrap:start",
            "geoNav:start",
            "worldActivation:start",
            "engines:start",
            "locationBootstrap:start",
            "spawn:start",
            "eventRuntime:start",
            "cleaning:start",
            "scheduledServices:start",
            "customEvents:start",
            "siegeSchedule:start",
            "dredgion:start",
            "battlefield:start",
            "protectorConqueror:start",
            "disputeLand:start",
            "html:start",
            "rewardServices:start",
            "runtimeServices:start",
            "optionalServices:start",
            "seasonRanking:start",
            "housing:start",
            "system:start:1",
            "networkStartup:start",
            "serverNetwork:start",
            "ratioLimit:start",
            "startupHooks:start",
            "startupCompletion:start:2"
        ), events);
    }

    private static final class RecordingGameThreadPoolLifecycle extends GameThreadPoolLifecycle {
    }

    private static final class RecordingGameStaticDataLifecycle extends GameStaticDataLifecycle {

        private final List<String> events;

        private RecordingGameStaticDataLifecycle(List<String> events) {
            super(new GameStaticDataGateway());
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
            super(new GameWorldBootstrapGateway());
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
            super(new GameEventBootstrapGateway());
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
            super(new GameGeoNavGateway());
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
            super(new GameWorldActivationGateway());
            this.events = events;
        }

        @Override
        public synchronized GameServer start() {
            events.add("worldActivation:start");
            return new GameServer();
        }
    }

    private static final class RecordingGameEnginesLifecycle extends GameEnginesLifecycle {

        private final List<String> events;

        private RecordingGameEnginesLifecycle(List<String> events) {
            super(new GameEnginesGateway());
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
            super(new GameLocationBootstrapGateway());
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
            super(new GameSpawnGateway());
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
            super(new GameEventRuntimeGateway());
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
            super(new GameCleaningGateway());
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
            super(new GameScheduledServicesGateway());
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
            super(new GameCustomEventsGateway());
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
            super(new GameSiegeScheduleGateway());
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
            super(new GameDredgionGateway());
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
            super(new GameBattlefieldGateway());
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
            super(new GameProtectorConquerorGateway());
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

    private static final class RecordingGameHousingLifecycle extends GameHousingLifecycle {

        private final List<String> events;

        private RecordingGameHousingLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("housing:start");
        }
    }

    private static final class RecordingGameSystemLifecycle extends GameSystemLifecycle {

        private final List<String> events;

        private RecordingGameSystemLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized long start(long startTimeMillis) {
            events.add("system:start:" + startTimeMillis);
            return 2;
        }
    }

    private static final class RecordingGameServerNetworkLifecycle extends GameServerNetworkLifecycle {

        private final List<String> events;

        private RecordingGameServerNetworkLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public void start(GameServer server) {
            events.add("serverNetwork:start");
        }
    }

    private static final class RecordingGameNetworkStartupLifecycle extends GameNetworkStartupLifecycle {

        private final List<String> events;

        private RecordingGameNetworkStartupLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start(Runnable serverStarter) {
            events.add("networkStartup:start");
            serverStarter.run();
        }
    }

    private static final class RecordingGameRatioLimitLifecycle extends GameRatioLimitLifecycle {

        private final List<String> events;

        private RecordingGameRatioLimitLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("ratioLimit:start");
        }
    }

    private static final class RecordingGameStartupHooksLifecycle extends GameStartupHooksLifecycle {

        private final List<String> events;

        private RecordingGameStartupHooksLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("startupHooks:start");
        }
    }

    private static final class RecordingGameStartupCompletionLifecycle extends GameStartupCompletionLifecycle {

        private final List<String> events;

        private RecordingGameStartupCompletionLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start(long startupTime) {
            events.add("startupCompletion:start:" + startupTime);
        }
    }

    private static final class RecordingGameLoggingLifecycle extends GameLoggingLifecycle {

        private final List<String> events;

        private RecordingGameLoggingLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("logging:start");
        }
    }

    private static final class RecordingGameUtilityServicesLifecycle extends GameUtilityServicesLifecycle {

        private final List<String> events;

        private RecordingGameUtilityServicesLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start(GameThreadPoolLifecycle threadPoolLifecycle) {
            events.add("utilityServices:start");
        }
    }

    private static final class RecordingGameAdminPanelLifecycle extends GameAdminPanelLifecycle {

        private final List<String> events;

        private RecordingGameAdminPanelLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("adminPanel:start");
        }
    }

    private static final class RecordingGameSystemPropertiesLifecycle extends GameSystemPropertiesLifecycle {

        private final List<String> events;

        private RecordingGameSystemPropertiesLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("systemProperties:start");
        }
    }

    private static final class RecordingGameStartupLogLifecycle extends GameStartupLogLifecycle {

        private final List<String> events;

        private RecordingGameStartupLogLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized long start() {
            events.add("startupLog:start");
            return 1;
        }
    }

    private static final class RecordingGameChatServerOverrideLifecycle extends GameChatServerOverrideLifecycle {

        private final List<String> events;

        private RecordingGameChatServerOverrideLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public synchronized void start(Boolean chatServerEnabledOverride) {
            events.add("chatOverride:start:" + chatServerEnabledOverride);
        }
    }
}
