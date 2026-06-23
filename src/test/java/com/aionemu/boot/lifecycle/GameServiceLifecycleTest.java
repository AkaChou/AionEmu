package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyConfigOverrides;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.lifecycle.GameAdminPanelLifecycle;
import com.aionemu.gameserver.lifecycle.GameBattlefieldGateway;
import com.aionemu.gameserver.lifecycle.GameBattlefieldLifecycle;
import com.aionemu.gameserver.lifecycle.GameChatServerOverrideLifecycle;
import com.aionemu.gameserver.lifecycle.GameCleaningGateway;
import com.aionemu.gameserver.lifecycle.GameCleaningLifecycle;
import com.aionemu.gameserver.lifecycle.GameCustomEventsGateway;
import com.aionemu.gameserver.lifecycle.GameCustomEventsLifecycle;
import com.aionemu.gameserver.lifecycle.GameDisputeLandGateway;
import com.aionemu.gameserver.lifecycle.GameDisputeLandLifecycle;
import com.aionemu.gameserver.lifecycle.GameDredgionGateway;
import com.aionemu.gameserver.lifecycle.GameDredgionLifecycle;
import com.aionemu.gameserver.lifecycle.GameEnginesGateway;
import com.aionemu.gameserver.lifecycle.GameEnginesLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameEventBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeGateway;
import com.aionemu.gameserver.lifecycle.GameEventRuntimeLifecycle;
import com.aionemu.gameserver.lifecycle.GameGeoNavGateway;
import com.aionemu.gameserver.lifecycle.GameGeoNavLifecycle;
import com.aionemu.gameserver.lifecycle.GameHtmlGateway;
import com.aionemu.gameserver.lifecycle.GameHtmlLifecycle;
import com.aionemu.gameserver.lifecycle.GameHousingGateway;
import com.aionemu.gameserver.lifecycle.GameHousingLifecycle;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapLifecycle;
import com.aionemu.gameserver.lifecycle.GameLoggingGateway;
import com.aionemu.gameserver.lifecycle.GameLoggingLifecycle;
import com.aionemu.gameserver.lifecycle.GameNetworkStartupLifecycle;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesGateway;
import com.aionemu.gameserver.lifecycle.GameOptionalServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorGateway;
import com.aionemu.gameserver.lifecycle.GameProtectorConquerorLifecycle;
import com.aionemu.gameserver.lifecycle.GameRatioLimitLifecycle;
import com.aionemu.gameserver.lifecycle.GameRewardServicesGateway;
import com.aionemu.gameserver.lifecycle.GameRewardServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesGateway;
import com.aionemu.gameserver.lifecycle.GameRuntimeServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingGateway;
import com.aionemu.gameserver.lifecycle.GameSeasonRankingLifecycle;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesGateway;
import com.aionemu.gameserver.lifecycle.GameScheduledServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameServerNetworkLifecycle;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleGateway;
import com.aionemu.gameserver.lifecycle.GameSiegeScheduleLifecycle;
import com.aionemu.gameserver.lifecycle.GameSpawnGateway;
import com.aionemu.gameserver.lifecycle.GameSpawnLifecycle;
import com.aionemu.gameserver.lifecycle.GameStaticDataGateway;
import com.aionemu.gameserver.lifecycle.GameStaticDataLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupCompletionLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupHooksGateway;
import com.aionemu.gameserver.lifecycle.GameStartupHooksLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupLogGateway;
import com.aionemu.gameserver.lifecycle.GameStartupLogLifecycle;
import com.aionemu.gameserver.lifecycle.GameStartupSequenceLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemGateway;
import com.aionemu.gameserver.lifecycle.GameSystemLifecycle;
import com.aionemu.gameserver.lifecycle.GameSystemPropertiesGateway;
import com.aionemu.gameserver.lifecycle.GameSystemPropertiesLifecycle;
import com.aionemu.gameserver.lifecycle.GameThreadPoolLifecycle;
import com.aionemu.gameserver.lifecycle.GameUtilityServicesLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldActivationGateway;
import com.aionemu.gameserver.lifecycle.GameWorldActivationLifecycle;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapGateway;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapLifecycle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.StandardEnvironment;

class GameServiceLifecycleTest {

    @Test
    void usesDirectLifecycleCollaboratorsInsteadOfActionAdapters() {
        assertEquals(GameStartupSequenceLifecycle.class, fieldType("startupSequenceLifecycle"));
        assertEquals(GameServerNetworkLifecycle.class, fieldType("serverNetworkLifecycle"));
        assertEquals(null, findFieldType("startAction"));
        assertEquals(null, findFieldType("stopAction"));
    }

    @Test
    void startAppliesLegacyConfigOverridesBeforeStartingGameServer() {
        AionServicesProperties services = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        LegacyConfigOverrides overrides = new RecordingLegacyConfigOverrides(events);
        GameThreadPoolLifecycle threadPoolLifecycle = new RecordingGameThreadPoolLifecycle(events);
        GameServiceLifecycle lifecycle = new GameServiceLifecycle(
            services,
            overrides,
            new RecordingGameStartupSequenceLifecycle(events),
            new RecordingGameServerNetworkLifecycle(events),
            threadPoolLifecycle
        );

        lifecycle.start(new DefaultApplicationArguments("--example=true"));

        assertEquals(List.of("apply", "start:false"), events);
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameServiceLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> findFieldType(String name) {
        try {
            Field field = GameServiceLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @Test
    void stopClosesThreadPoolAfterStoppingGameServer() {
        AionServicesProperties services = new AionServicesProperties();
        List<String> events = new ArrayList<>();
        LegacyConfigOverrides overrides = new RecordingLegacyConfigOverrides(events);
        GameThreadPoolLifecycle threadPoolLifecycle = new RecordingGameThreadPoolLifecycle(events);
        GameServiceLifecycle lifecycle = new GameServiceLifecycle(
            services,
            overrides,
            new RecordingGameStartupSequenceLifecycle(events),
            new RecordingGameServerNetworkLifecycle(events),
            threadPoolLifecycle
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

    private static final class RecordingGameStartupSequenceLifecycle extends GameStartupSequenceLifecycle {

        private final List<String> events;

        private RecordingGameStartupSequenceLifecycle(List<String> events) {
            super(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
            this.events = events;
        }

        @Override
        public void start(Boolean chatServerEnabledOverride) {
            events.add("start:" + chatServerEnabledOverride);
        }
    }

    private static final class RecordingGameServerNetworkLifecycle extends GameServerNetworkLifecycle {

        private final List<String> events;

        private RecordingGameServerNetworkLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        public void stop() {
            events.add("stop");
        }
    }

    private static final class RecordingGameLoggingLifecycle extends GameLoggingLifecycle {

        private final List<String> events;

        private RecordingGameLoggingLifecycle(List<String> events) {
            super(new GameLoggingGateway());
            this.events = events;
        }

        @Override
        public synchronized void start() {
            events.add("logging:start");
        }
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
            super(new GameDisputeLandGateway());
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
            super(new GameHtmlGateway());
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
            super(new GameRewardServicesGateway());
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
            super(new GameRuntimeServicesGateway());
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
            super(new GameOptionalServicesGateway());
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
            super(new GameSeasonRankingGateway());
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
            super(new GameHousingGateway());
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
            super(new GameSystemGateway());
            this.events = events;
        }

        @Override
        public synchronized long start(long serverStartTimeMillis) {
            events.add("system:start");
            return 1;
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
            super(new GameStartupHooksGateway());
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
            super(new GameSystemPropertiesGateway());
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
            super(new GameStartupLogGateway());
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
