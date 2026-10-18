package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * R5 冻结审计：17 个剩余静态定位器已退役，绑定职责由 16 个 XxxServiceBindings 承担。
 * R5 freeze audit: the 17 remaining static locators are retired; the binding duty moved to the 16
 * XxxServiceBindings components. Consumers use each domain service's fail-fast getInstance() channel.
 */
class RemainingServiceBindingsTest {

    /** 已退役定位器 → 承接绑定的组件。 / Retired locator to its bindings component. */
    private static final Map<String, String> RETIRED_LOCATORS = Map.ofEntries(
        Map.entry("GameMaintenanceServices", "MaintenanceServiceBindings"),
        Map.entry("GameCraftServices", "CraftServiceBindings"),
        Map.entry("GameEventServices", "EventServiceBindings"),
        Map.entry("GameMovementLoopServices", "MovementLoopServiceBindings"),
        Map.entry("GameStaticDataServices", "StaticDataServiceBindings"),
        Map.entry("GameGameplayServices", "GameplayServiceBindings"),
        Map.entry("GameTaskManagerServices", "TaskManagerServiceBindings"),
        Map.entry("GameCreativityServices", "CreativityServiceBindings"),
        Map.entry("GameBattlefieldServices", "BattlefieldServiceBindings"),
        Map.entry("GameEventBootstrapServices", "EventBootstrapServiceBindings"),
        Map.entry("GameServerNetworkServices", "ServerNetworkServiceBindings"),
        Map.entry("GameHousingServices", "HousingServiceBindings"),
        Map.entry("GameRuntimeServices", "RuntimeServiceBindings"),
        Map.entry("GameEngineServices", "EngineServiceBindings"),
        Map.entry("GameWorldBootstrapServices", "WorldBootstrapServiceBindings"),
        Map.entry("GameThreadPoolServices", "ThreadPoolServiceBindings"));

    /** 绑定组件 → 必须喂入 fail-fast 单例通道的服务集。 / Bindings component to the services whose fail-fast singleton channels it must feed. */
    private static final Map<String, List<String>> BINDINGS_FEEDS = Map.ofEntries(
        Map.entry("MaintenanceServiceBindings", List.of("SeasonRankingUpdateService", "ShugoImperialTombSpawnManager")),
        Map.entry("CraftServiceBindings", List.of("CraftSkillUpdateService", "RelinquishCraftStatus")),
        Map.entry("EventServiceBindings", List.of("EventService", "PlayerEventService", "CrazyDaevaService", "AbyssRankUpdateService", "PacketBroadcaster", "EventScheduler")),
        Map.entry("MovementLoopServiceBindings", List.of("MovementNotifyTask", "MoveTaskManager", "PlayerMoveTaskManager", "ZoneUpdateService")),
        Map.entry("StaticDataServiceBindings", List.of("DataManager", "HTMLCache", "XmlDataLoader")),
        Map.entry("GameplayServiceBindings", List.of("DuelService", "LifeStatsRestoreService", "SeasonRankingService", "RiftManager")),
        Map.entry("TaskManagerServiceBindings", List.of("ExpireTimerTask", "TeamEffectUpdater", "TeamMoveUpdater", "TemporaryTradeTimeTask")),
        Map.entry("CreativityServiceBindings", List.of("CreativityEssenceService", "CreativitySkillService", "CreativityStatsService", "CreativityTransfoService", "Accuracy", "Agility", "Health", "Knowledge", "Power", "Precision", "Will")),
        Map.entry("BattlefieldServiceBindings", List.of("KamarBattlefieldService", "EngulfedOphidanBridgeService", "SuspiciousOphidanBridgeService", "IronWallWarfrontService", "IdgelDomeService", "IdgelDomeLandmarkService", "HallOfTenacityService", "GrandArenaTrainingCampService", "IDRunService")),
        Map.entry("EventBootstrapServiceBindings", List.of("LunaShopService", "MinionService", "ShugoSweepService", "AtreianPassportService")),
        Map.entry("ServerNetworkServiceBindings", List.of("LoginServer", "ChatServer", "BannedMacManager", "NetworkController", "PacketLoggerService", "AionPacketHandlerFactory", "PacketFloodFilter", "LsPacketHandlerFactory")),
        Map.entry("HousingServiceBindings", List.of("HousingService", "HousingBidService", "TownService", "MaintenanceTask", "ChallengeTaskService")),
        Map.entry("RuntimeServiceBindings", List.of("InGameShopEn", "FindGroupService", "BrokerService", "Influence", "ExchangeService", "WeatherService", "PetitionService", "PlayerTransferService", "TerritoryService", "LimitedItemTradeService", "AdminService", "SurveyService", "GMService", "PeriodicSaveService", "AnnouncementService", "BoostEventService")),
        Map.entry("EngineServiceBindings", List.of("SkillEngine", "QuestEngine", "InstanceEngine", "AI2Engine", "ChatProcessor")),
        Map.entry("WorldBootstrapServiceBindings", List.of("World", "IDFactory", "ZoneService", "HotspotTeleportService")),
        Map.entry("ThreadPoolServiceBindings", List.of("ThreadPoolManager")));

    @Test
    void retiredLocatorClassesDoNotComeBack() {
        for (String locator : new String[] {"GameMaintenanceServices.java", "GameCraftServices.java", "GameEventServices.java", "GameMovementLoopServices.java", "GameStaticDataServices.java", "GameGameplayServices.java", "GameTaskManagerServices.java", "GameCreativityServices.java", "GameBattlefieldServices.java", "GameEventBootstrapServices.java", "GameServerNetworkServices.java", "GameCronServices.java", "GameHousingServices.java", "GameRuntimeServices.java", "GameEngineServices.java", "GameWorldBootstrapServices.java", "GameThreadPoolServices.java"}) {
            assertFalse(
                Files.exists(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/" + locator)),
                locator + " 定位器已退役，不得重新引入（R5，绑定职责见对应 XxxServiceBindings）");
        }
    }

    @Test
    void productionCodeNeverTouchesTheRetiredLocators() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java"))) {
            for (Path source : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source);
                for (String locator : RETIRED_LOCATORS.keySet()) {
                    assertFalse(Pattern.compile("\\b" + locator + "\\b").matcher(content).find(),
                        source + " 仍引用已退役定位器 " + locator);
                }
                assertFalse(Pattern.compile("\\bGameCronServices\\b").matcher(content).find(),
                    source + " 仍引用已退役定位器 GameCronServices");
            }
        }
    }

    @Test
    void bindingsComponentsFeedEveryDomainServiceChannel() throws IOException {
        for (Map.Entry<String, List<String>> entry : BINDINGS_FEEDS.entrySet()) {
            String source = Files.readString(
                Path.of("src/main/java/com/aionemu/gameserver/lifecycle/" + entry.getKey() + ".java"));
            for (String service : entry.getValue()) {
                assertTrue(source.contains(service + ".setInstanceProvider("),
                    entry.getKey() + " 必须喂入 " + service + " 的 fail-fast 单例通道");
                assertTrue(source.contains(service + ".setInstanceProvider(null)"),
                    entry.getKey() + " 销毁时必须清空 " + service + " 的静态提供者通道");
            }
        }
    }
}
