package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Spring 迁移护栏：双源静态兜底已全部退役，冻结"零回落"并防止退役类复发。
 * Spring-migration guard: the dual-source static fallbacks are fully retired; freezes "zero fallback"
 * and prevents retired classes from regressing.
 * <p>背景：一次未彻底完成的 Spring 改造留下了
 * {@code provider.getIfAvailable(() -> SingletonHolder.instance)}
 * 这种"Spring 优先、静态兜底"的双源写法；兜底一旦被走到，就会在容器之外静默创建第二套实例。
 * 131 个类已全部改为 fail-fast，本测试冻结这份清单：回落点数量必须保持 0，且退役类不得再出现回落或静态兜底定义。
 * All 131 dual-source classes now fail fast; this test freezes the inventory: the fallback count must stay 0
 * and retired classes must not reintroduce a fallback or a SingletonHolder definition.</p>
 */
class LegacySingletonFallbackAuditTest {

	/** 已退役双源兜底的文件清单（相对 src/main/java）。 / Retired dual-source fallback files. */
	private static final Set<String> RETIRED = Set.of(
		"com/aionemu/gameserver/ai2/AI2Engine.java",
		"com/aionemu/gameserver/cache/HTMLCache.java",
		"com/aionemu/gameserver/dataholders/DataManager.java",
		"com/aionemu/gameserver/dataholders/loadingutils/XmlDataLoader.java",
		"com/aionemu/gameserver/eventEngine/EventScheduler.java",
		"com/aionemu/gameserver/instance/InstanceEngine.java",
		"com/aionemu/gameserver/model/house/MaintenanceTask.java",
		"com/aionemu/gameserver/model/ingameshop/InGameShopEn.java",
		"com/aionemu/gameserver/model/siege/Influence.java",
		"com/aionemu/gameserver/network/PacketFloodFilter.java",
		"com/aionemu/gameserver/network/PacketLoggerService.java",
		"com/aionemu/gameserver/network/chatserver/ChatServer.java",
		"com/aionemu/gameserver/network/factories/AionPacketHandlerFactory.java",
		"com/aionemu/gameserver/network/factories/LsPacketHandlerFactory.java",
		"com/aionemu/gameserver/network/loginserver/LoginServer.java",
		"com/aionemu/gameserver/questEngine/QuestEngine.java",
		"com/aionemu/gameserver/services/AStationService.java",
		"com/aionemu/gameserver/services/AbyssLandingService.java",
		"com/aionemu/gameserver/services/AbyssLandingSpecialService.java",
		"com/aionemu/gameserver/services/AdminService.java",
		"com/aionemu/gameserver/services/AnnouncementService.java",
		"com/aionemu/gameserver/services/AutoGroupService.java",
		"com/aionemu/gameserver/services/BrokerService.java",
		"com/aionemu/gameserver/services/ChallengeTaskService.java",
		"com/aionemu/gameserver/services/CuringZoneService.java",
		"com/aionemu/gameserver/services/DatabaseCleaningService.java",
		"com/aionemu/gameserver/services/DebugService.java",
		"com/aionemu/gameserver/services/DuelService.java",
		"com/aionemu/gameserver/services/EventService.java",
		"com/aionemu/gameserver/services/ExchangeService.java",
		"com/aionemu/gameserver/services/F2pService.java",
		"com/aionemu/gameserver/services/FindGroupService.java",
		"com/aionemu/gameserver/services/FlyRingService.java",
		"com/aionemu/gameserver/services/GameTimeService.java",
		"com/aionemu/gameserver/services/HousingBidService.java",
		"com/aionemu/gameserver/services/HousingService.java",
		"com/aionemu/gameserver/services/KiskService.java",
		"com/aionemu/gameserver/services/LegionService.java",
		"com/aionemu/gameserver/services/LimitedItemTradeService.java",
		"com/aionemu/gameserver/services/MotionLoggingService.java",
		"com/aionemu/gameserver/services/NpcShoutsService.java",
		"com/aionemu/gameserver/services/PeriodicSaveService.java",
		"com/aionemu/gameserver/services/PetitionService.java",
		"com/aionemu/gameserver/services/ProtectorConquerorService.java",
		"com/aionemu/gameserver/services/PvpService.java",
		"com/aionemu/gameserver/services/RepurchaseService.java",
		"com/aionemu/gameserver/services/RoadService.java",
		"com/aionemu/gameserver/services/ShieldService.java",
		"com/aionemu/gameserver/services/SpringZoneService.java",
		"com/aionemu/gameserver/services/StaticDoorService.java",
		"com/aionemu/gameserver/services/SurveyService.java",
		"com/aionemu/gameserver/services/TownService.java",
		"com/aionemu/gameserver/services/WeatherService.java",
		"com/aionemu/gameserver/services/WebshopService.java",
		"com/aionemu/gameserver/services/WindyGorgeService.java",
		"com/aionemu/gameserver/services/abyss/AbyssRankCleaningService.java",
		"com/aionemu/gameserver/services/abyss/AbyssRankUpdateService.java",
		"com/aionemu/gameserver/services/abyss/AbyssRankingCache.java",
		"com/aionemu/gameserver/services/abysslandingservice/LandingUpdateService.java",
		"com/aionemu/gameserver/services/craft/CraftSkillUpdateService.java",
		"com/aionemu/gameserver/services/craft/RelinquishCraftStatus.java",
		"com/aionemu/gameserver/services/drop/DropDistributionService.java",
		"com/aionemu/gameserver/services/drop/DropRegistrationService.java",
		"com/aionemu/gameserver/services/drop/DropService.java",
		"com/aionemu/gameserver/services/events/ArcadeUpgradeService.java",
		"com/aionemu/gameserver/services/events/AtreianPassportService.java",
		"com/aionemu/gameserver/services/events/BGService.java",
		"com/aionemu/gameserver/services/events/BanditService.java",
		"com/aionemu/gameserver/services/events/BoostEventService.java",
		"com/aionemu/gameserver/services/events/CrazyDaevaService.java",
		"com/aionemu/gameserver/services/events/EventWindowService.java",
		"com/aionemu/gameserver/services/events/FFAService.java",
		"com/aionemu/gameserver/services/events/LadderService.java",
		"com/aionemu/gameserver/services/events/ShugoSweepService.java",
		"com/aionemu/gameserver/services/events/ThievesGuildService.java",
		"com/aionemu/gameserver/services/instance/AsyunatarService.java",
		"com/aionemu/gameserver/services/instance/DredgionService2.java",
		"com/aionemu/gameserver/services/instance/EngulfedOphidanBridgeService.java",
		"com/aionemu/gameserver/services/instance/GrandArenaTrainingCampService.java",
		"com/aionemu/gameserver/services/instance/HallOfTenacityService.java",
		"com/aionemu/gameserver/services/instance/IDRunService.java",
		"com/aionemu/gameserver/services/instance/IdgelDomeLandmarkService.java",
		"com/aionemu/gameserver/services/instance/IdgelDomeService.java",
		"com/aionemu/gameserver/services/instance/IronWallWarfrontService.java",
		"com/aionemu/gameserver/services/instance/KamarBattlefieldService.java",
		"com/aionemu/gameserver/services/instance/SuspiciousOphidanBridgeService.java",
		"com/aionemu/gameserver/services/item/CoalescenceService.java",
		"com/aionemu/gameserver/services/mail/MailService.java",
		"com/aionemu/gameserver/services/mail/SystemMailService.java",
		"com/aionemu/gameserver/services/player/AtreianBestiaryService.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/CreativityEssenceService.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/CreativitySkillService.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/CreativityStatsService.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/CreativityTransfoService.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/stats/Accuracy.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/stats/Agility.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/stats/Health.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/stats/Knowledge.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/stats/Power.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/stats/Precision.java",
		"com/aionemu/gameserver/services/player/CreativityPanel/stats/Will.java",
		"com/aionemu/gameserver/services/player/GrowthEnergy.java",
		"com/aionemu/gameserver/services/player/LunaShopService.java",
		"com/aionemu/gameserver/services/player/PlayerEventService.java",
		"com/aionemu/gameserver/services/player/PlayerLimitService.java",
		"com/aionemu/gameserver/services/ranking/SeasonRankingService.java",
		"com/aionemu/gameserver/services/ranking/SeasonRankingUpdateService.java",
		"com/aionemu/gameserver/services/reward/RewardService.java",
		"com/aionemu/gameserver/services/teleport/HotspotTeleportService.java",
		"com/aionemu/gameserver/services/territory/TerritoryService.java",
		"com/aionemu/gameserver/services/toypet/MinionService.java",
		"com/aionemu/gameserver/services/toypet/PetService.java",
		"com/aionemu/gameserver/services/transfers/PlayerTransferService.java",
		"com/aionemu/gameserver/services/veteranreward/VeteranRewardsService.java",
		"com/aionemu/gameserver/spawnengine/ShugoImperialTombSpawnManager.java",
		"com/aionemu/gameserver/taskmanager/TaskManagerFromDB.java",
		"com/aionemu/gameserver/taskmanager/tasks/ExpireTimerTask.java",
		"com/aionemu/gameserver/taskmanager/tasks/MoveTaskManager.java",
		"com/aionemu/gameserver/taskmanager/tasks/MovementNotifyTask.java",
		"com/aionemu/gameserver/taskmanager/tasks/PacketBroadcaster.java",
		"com/aionemu/gameserver/taskmanager/tasks/PlayerMoveTaskManager.java",
		"com/aionemu/gameserver/taskmanager/tasks/TeamEffectUpdater.java",
		"com/aionemu/gameserver/taskmanager/tasks/TeamMoveUpdater.java",
		"com/aionemu/gameserver/taskmanager/tasks/TemporaryTradeTimeTask.java",
		"com/aionemu/gameserver/utils/ThreadPoolManager.java",
		"com/aionemu/gameserver/utils/audit/GMService.java",
		"com/aionemu/gameserver/utils/idfactory/IDFactory.java",
		"com/aionemu/gameserver/world/World.java",
		"com/aionemu/gameserver/world/geo/GeoService.java",
		"com/aionemu/gameserver/world/zone/ZoneService.java",
		"com/aionemu/gameserver/world/zone/ZoneUpdateService.java");

	@Test
	void dualSourceFallbacksStayRetired() throws IOException {
		Set<String> seen = new HashSet<>();
		int sites = 0;
		try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				String relative = file.toString().substring("src/main/java/".length()).replace('\\', '/');
				String source = Files.readString(file);
				if (RETIRED.contains(relative)) {
					seen.add(relative);
					assertFalse(source.contains("getIfAvailable(() ->"),
						"已退役双源兜底的类不得再出现 provider 回落：" + file);
					assertFalse(source.contains("class SingletonHolder"),
						"已退役双源兜底的类不得再出现静态兜底持有者：" + file);
					continue;
				}
				sites += (int) source.lines()
					.filter(line -> line.contains("getIfAvailable(() ->") && line.contains("SingletonHolder"))
					.count();
			}
		}
		assertEquals(RETIRED, seen, "RETIRED 清单中的每个文件都必须存在");
		assertEquals(0, sites, "双源静态兜底必须保持为零；新增会重新引入容器之外的第二套实例");
	}
}
