package com.aionemu.gameserver.services.teleport;


import java.util.Set;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerInitialData;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.BindPointPosition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.portal.InstanceExit;
import com.aionemu.gameserver.model.templates.portal.PortalLoc;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalScroll;
import com.aionemu.gameserver.model.templates.revive_start_points.InstanceReviveStartPoints;
import com.aionemu.gameserver.model.templates.revive_start_points.WorldReviveStartPoints;
import com.aionemu.gameserver.model.templates.robot.RobotInfo;
import com.aionemu.gameserver.model.templates.spawns.SpawnSearchResult;
import com.aionemu.gameserver.model.templates.spawns.SpawnSpotTemplate;
import com.aionemu.gameserver.model.templates.teleport.TelelocationTemplate;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.model.templates.teleport.TeleportType;
import com.aionemu.gameserver.model.templates.teleport.TeleporterTemplate;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_A_STATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_A_STATION_MOVE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BIND_POINT_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHANNEL_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CONQUEROR_PROTECTOR;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_SPAWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TELEPORT_LOC;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TELEPORT_MAP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_USE_ROBOT;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.services.PrivateStoreService;
import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.services.trade.PricesService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapType;
import com.aionemu.gameserver.world.WorldPosition;
/**
 * 传送服务，提供玩家传送、回城、监狱、频道切换、变形同步与跨服 A-Station 等入口。
 * Teleport service providing player travel, bind/prison, channel switch, transform sync and A-Station moves.
 */
@Slf4j

public class TeleportService2 {

	private static final int TELEPORT_DEFAULT_DELAY = 2200;
	private static final int BEAM_DEFAULT_DELAY = 3000;
	private static final int ABYSS_ENTRY_WORLD_ID = 400010000;
	// 入场资格取决于系列任务终点：天族 1920->1921->1922->1044，魔族 2945->2946->2947->2042。
	// Entry qualification is the terminal quest of each chain: 1920->1921->1922->1044 and 2945->2946->2947->2042.
	private static final int ELYOS_ABYSS_ENTRY_QUEST_ID = 1044;
	private static final int ASMODIAN_ABYSS_ENTRY_QUEST_ID = 2042;
	private static final int ELYOS_INGGISON_WORLD_ID = 210050000;
	private static final int ELYOS_INGGISON_MASTER_WORLD_ID = 210130000;
	private static final int ASMODIAN_GELKMAROS_WORLD_ID = 220070000;
	private static final int ASMODIAN_GELKMAROS_MASTER_WORLD_ID = 220140000;
	private static final int ELYOS_BALAUREA_ENTRY_QUEST_ID = 10031;
	private static final int ASMODIAN_BALAUREA_ENTRY_QUEST_ID = 20031;
	private static final int ELYOS_BALAUREA_ENTRY_QUEST_STEP = 3;
	private static final int ASMODIAN_BALAUREA_ENTRY_QUEST_STEP = 3;
	private static final int KAHRUN_WORLD_ID = 600100000;
	// 任务推进到传送到哥尔哈的步骤后即永久开放常规交通。
	// Reaching the quest step that teleports the player to Kahrun permanently unlocks regular travel there.
	private static final int ELYOS_KAHRUN_ENTRY_QUEST_ID = 10100;
	private static final int ASMODIAN_KAHRUN_ENTRY_QUEST_ID = 20100;
	private static final int KAHRUN_ENTRY_QUEST_STEP = 1;
	private static final int ELYOS_ILUMA_WORLD_ID = 210100000;
	private static final int ASMODIAN_NORSVOLD_WORLD_ID = 220110000;
	// 任务推进到传送到伊鲁玛/诺斯珀德的步骤后即开放本阵营新大陆交通。
	// Reaching the teleport step of the level-65 ArchDaeva mission unlocks the racial continent.
	private static final int ELYOS_ARCHDAEVA_ENTRY_QUEST_ID = 10520;
	private static final int ASMODIAN_ARCHDAEVA_ENTRY_QUEST_ID = 20520;
	private static final int ARCHDAEVA_ENTRY_QUEST_STEP = 4;
	/**
	 * 配置出口不在欧比斯、但副本内部传送 NPC 直达欧比斯的世界 ID。
	 * Instance worlds whose configured exit points elsewhere while an in-instance portal NPC leads into the Abyss.
	 */
	private static final Set<Integer> ABYSS_PORTAL_INSTANCE_WORLD_IDS = Set.of(300040000);

	/**
	 * 按传送员模板将玩家传送到指定地点（含飞行传送与费用校验）。
	 * Teleports a player via teleporter template to a location (flight travel and fee checks included).
	 *
	 * @param template 传送员模板 / Teleporter template
	 * @param locId 目标地点 ID / Target location id
	 * @param player 玩家 / Player
	 * @param npc 交互 NPC / Interacting NPC
	 * @param animation 传送动画 / Teleport animation
	 */
	public static void teleport(TeleporterTemplate template, int locId, Player player, Npc npc, TeleportAnimation animation) {
		TribeClass tribe = npc.getTribe();
		Race race = player.getRace();
		if (tribe.equals(TribeClass.FIELD_OBJECT_LIGHT) && race.equals(Race.ASMODIANS) || (tribe.equals(TribeClass.FIELD_OBJECT_DARK) && race.equals(Race.ELYOS))) {
			return;
		}

		if (template.getTeleLocIdData() == null) {
			log.info(I18n.get("log.125a3801052b", locId));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE);
			if (player.isGM()) {
				PacketSendUtility.sendMessage(player, "Missing locId for this teleporter at teleporter_templates.xml with locId: " + locId);
			}
			return;
		}

		TeleportLocation location = template.getTeleLocIdData().getTeleportLocation(locId);

		if (location == null) {
			log.info(I18n.get("log.125a3801052b", locId));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE);
			if (player.isGM()) {
				PacketSendUtility.sendMessage(player, "Missing locId for this teleporter at teleporter_templates.xml with locId: " + locId);
			}
			return;
		}

		TelelocationTemplate locationTemplate = DataManager.TELELOCATION_DATA.getTelelocationTemplate(locId);

		if (locationTemplate == null) {
			log.info(I18n.get("log.1e833c006a12", locId));
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NO_ROUTE);
			if (player.isGM()) {
				PacketSendUtility.sendMessage(player, "Missing info at teleport_location.xml with locId: " + locId);
			}
			return;
		}

		if (isKahrunEntryWorld(locationTemplate.getMapId()) && !meetsKahrunEntryRequirement(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}

		if (isArchDaevaEntryWorld(locationTemplate.getMapId())
				&& !meetsArchDaevaEntryRequirement(player, locationTemplate.getMapId())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}

		if (location.getRequiredQuest() > 0) {
			QuestState qs = player.getQuestStateList().getQuestState(location.getRequiredQuest());
			if (!meetsQuestRequirement(qs, location.getRequiredQuestStep())) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
				return;
			}
		}

		if (!checkKinahForTransportation(location, player)) {
			return;
		}

		if (location.getType() == TeleportType.FLIGHT) {
			SummonsService.suspendForTeleport(player);
			player.unsetPlayerMode(PlayerMode.RIDE);
			player.setState(CreatureState.FLIGHT_TELEPORT);
			player.unsetState(CreatureState.ACTIVE);
			player.setFlightTeleportId(location.getTeleportId());
			PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.START_FLYTELEPORT, location.getTeleportId(), 0), true);
			playerTransformation(player);
			instanceTransformation(player);
			archdaevaTransformation(player);
		} else {
			int instanceId = 1;
			int mapId = locationTemplate.getMapId();
			if (player.getWorldId() == mapId) {
				instanceId = player.getInstanceId();
			}
			sendLoc(player, mapId, instanceId, locationTemplate.getX(), locationTemplate.getY(), locationTemplate.getZ(), (byte) locationTemplate.getHeading(), animation);
			playerTransformation(player);
			instanceTransformation(player);
			archdaevaTransformation(player);
		}
	}

	static boolean meetsQuestRequirement(QuestState questState, int requiredQuestStep) {
		if (questState == null) {
			return false;
		}
		if (questState.getStatus() == QuestStatus.COMPLETE) {
			return true;
		}
		return requiredQuestStep > 0
				&& (questState.getStatus() == QuestStatus.START || questState.getStatus() == QuestStatus.REWARD)
				&& questState.getQuestVarById(0) >= requiredQuestStep;
	}

	/**
	 * 判断目标世界是否为欧比斯入口世界。
	 * Returns whether the target world is the Abyss entry world.
	 *
	 * @param worldId 世界 ID / World id
	 * @return 是否为欧比斯 / whether it is the Abyss
	 */
	public static boolean isAbyssEntryWorld(int worldId) {
		return worldId == ABYSS_ENTRY_WORLD_ID;
	}

	/**
	 * 检查玩家是否已完成本阵营的欧比斯入场任务。
	 * Checks whether the player completed the racial Abyss entry quest.
	 *
	 * @param player 玩家 / Player
	 * @return 是否允许进入欧比斯 / whether Abyss entry is allowed
	 */
	public static boolean meetsAbyssEntryRequirement(Player player) {
		int questId = getAbyssEntryQuestId(player.getRace());
		return questId != 0 && meetsQuestRequirement(player.getQuestStateList().getQuestState(questId), 0);
	}

	static boolean meetsAbyssEntryRequirement(Race race, QuestState questState) {
		int questId = getAbyssEntryQuestId(race);
		return questId != 0 && questState != null && questState.getQuestId() == questId
				&& meetsQuestRequirement(questState, 0);
	}

	static int getAbyssEntryQuestId(Race race) {
		return switch (race) {
			case ELYOS -> ELYOS_ABYSS_ENTRY_QUEST_ID;
			case ASMODIANS -> ASMODIAN_ABYSS_ENTRY_QUEST_ID;
			default -> 0;
		};
	}

	public static boolean isInggisonEntryWorld(int worldId) {
		return worldId == ELYOS_INGGISON_WORLD_ID || worldId == ELYOS_INGGISON_MASTER_WORLD_ID;
	}

	public static boolean isGelkmarosEntryWorld(int worldId) {
		return worldId == ASMODIAN_GELKMAROS_WORLD_ID || worldId == ASMODIAN_GELKMAROS_MASTER_WORLD_ID;
	}

	public static boolean isBalaureaEntryWorld(int worldId) {
		return isInggisonEntryWorld(worldId) || isGelkmarosEntryWorld(worldId);
	}

	/**
	 * 将英吉斯温镜像服世界 ID 归一为实际可玩的英吉斯温世界。
	 * Normalizes the Inggison mirror-server world id to the live Inggison world.
	 *
	 * @param worldId 目标世界 ID / Target world id
	 * @return 实际可玩世界 ID / Live world id
	 */
	static int resolveInggisonWorldId(int worldId) {
		return worldId == ELYOS_INGGISON_MASTER_WORLD_ID ? ELYOS_INGGISON_WORLD_ID : worldId;
	}

	public static boolean meetsBalaureaEntryRequirement(Player player, int targetWorldId) {
		if (isInggisonEntryWorld(targetWorldId)) {
			return meetsBalaureaEntryRequirement(Race.ELYOS, player.getRace(),
					player.getQuestStateList().getQuestState(ELYOS_BALAUREA_ENTRY_QUEST_ID));
		}
		if (isGelkmarosEntryWorld(targetWorldId)) {
			return meetsBalaureaEntryRequirement(Race.ASMODIANS, player.getRace(),
					player.getQuestStateList().getQuestState(ASMODIAN_BALAUREA_ENTRY_QUEST_ID));
		}
		return false;
	}

	static boolean meetsBalaureaEntryRequirement(Race targetRace, Race playerRace, QuestState questState) {
		if (playerRace != targetRace) {
			return false;
		}
		int questId = getBalaureaEntryQuestId(targetRace);
		int requiredStep = getBalaureaEntryQuestStep(targetRace);
		return questId != 0 && questState != null && questState.getQuestId() == questId
				&& meetsQuestRequirement(questState, requiredStep);
	}

	static int getBalaureaEntryQuestId(Race race) {
		return switch (race) {
			case ELYOS -> ELYOS_BALAUREA_ENTRY_QUEST_ID;
			case ASMODIANS -> ASMODIAN_BALAUREA_ENTRY_QUEST_ID;
			default -> 0;
		};
	}

	static int getBalaureaEntryQuestStep(Race race) {
		return switch (race) {
			case ELYOS -> ELYOS_BALAUREA_ENTRY_QUEST_STEP;
			case ASMODIANS -> ASMODIAN_BALAUREA_ENTRY_QUEST_STEP;
			default -> 0;
		};
	}

	/**
	 * 判断目标世界是否为哥尔哈。
	 * Returns whether the target world is Kahrun.
	 *
	 * @param worldId 世界 ID / World id
	 * @return 是否为哥尔哈 / whether it is Kahrun
	 */
	public static boolean isKahrunEntryWorld(int worldId) {
		return worldId == KAHRUN_WORLD_ID;
	}

	/**
	 * 检查玩家是否已通过本阵营 65 级使命抵达哥尔哈。
	 * Checks whether the player has reached Kahrun through the racial level-65 mission.
	 *
	 * @param player 玩家 / Player
	 * @return 是否允许前往哥尔哈 / whether Kahrun travel is allowed
	 */
	public static boolean meetsKahrunEntryRequirement(Player player) {
		int questId = getKahrunEntryQuestId(player.getRace());
		return questId != 0 && meetsQuestRequirement(player.getQuestStateList().getQuestState(questId),
				KAHRUN_ENTRY_QUEST_STEP);
	}

	static boolean meetsKahrunEntryRequirement(Race race, QuestState questState) {
		int questId = getKahrunEntryQuestId(race);
		return questId != 0 && questState != null && questState.getQuestId() == questId
				&& meetsQuestRequirement(questState, KAHRUN_ENTRY_QUEST_STEP);
	}

	static int getKahrunEntryQuestId(Race race) {
		return switch (race) {
			case ELYOS -> ELYOS_KAHRUN_ENTRY_QUEST_ID;
			case ASMODIANS -> ASMODIAN_KAHRUN_ENTRY_QUEST_ID;
			default -> 0;
		};
	}

	/**
	 * 判断目标世界是否为本阵营高阶守护者新大陆（伊鲁玛/诺斯珀德）。
	 * Returns whether the target world is a racial ArchDaeva continent (Iluma/Norsvold).
	 *
	 * @param worldId 目标世界 ID / Target world id
	 * @return 是否为高阶守护者新大陆 / whether it is an ArchDaeva continent
	 */
	public static boolean isArchDaevaEntryWorld(int worldId) {
		return worldId == ELYOS_ILUMA_WORLD_ID || worldId == ASMODIAN_NORSVOLD_WORLD_ID;
	}

	/**
	 * 检查玩家能否进入目标高阶守护者新大陆。
	 * 本阵营世界必须由该阵营 10520/20520 的传送步骤解锁；敌对阵营沿用裂隙等既有规则。
	 * Checks whether the player may enter the target ArchDaeva continent. A race's home world requires
	 * that race's 10520/20520 teleport step; the opposite race keeps its existing rift rules.
	 *
	 * @param player 玩家 / Player
	 * @param targetWorldId 目标世界 ID / Target world id
	 * @return 是否允许进入 / whether entry is allowed
	 */
	public static boolean meetsArchDaevaEntryRequirement(Player player, int targetWorldId) {
		int questId = getArchDaevaEntryQuestId(player.getRace(), targetWorldId);
		return questId == 0 || meetsQuestRequirement(player.getQuestStateList().getQuestState(questId),
				ARCHDAEVA_ENTRY_QUEST_STEP);
	}

	static boolean meetsArchDaevaEntryRequirement(Race race, int targetWorldId, QuestState questState) {
		int questId = getArchDaevaEntryQuestId(race, targetWorldId);
		return questId == 0 || (questState != null && questState.getQuestId() == questId
				&& meetsQuestRequirement(questState, ARCHDAEVA_ENTRY_QUEST_STEP));
	}

	static int getArchDaevaEntryQuestId(Race race, int targetWorldId) {
        return switch (race) {
            case ELYOS -> targetWorldId == ELYOS_ILUMA_WORLD_ID ? ELYOS_ARCHDAEVA_ENTRY_QUEST_ID : 0;
            case ASMODIANS -> targetWorldId == ASMODIAN_NORSVOLD_WORLD_ID ? ASMODIAN_ARCHDAEVA_ENTRY_QUEST_ID : 0;
            default -> 0;
        };
	}

	/**
	 * 判断副本的配置出口是否位于欧比斯。
	 * Returns whether the instance's configured exit leads into the Abyss.
	 *
	 * @param worldId 副本世界 ID / Instance world id
	 * @param race 玩家种族 / Player race
	 * @return 是否出口通往欧比斯 / whether the exit leads into the Abyss
	 */
	public static boolean isAbyssExitInstance(int worldId, Race race) {
		if (DataManager.INSTANCE_EXIT_DATA == null) {
			return false;
		}
		InstanceExit exit = getInstanceExit(worldId, race);
		return exit != null && isAbyssEntryWorld(exit.getExitWorld());
	}

	/**
	 * 判断进入该副本是否可能获得通往欧比斯的入口（配置出口或在副本内部传送门）。
	 * Returns whether entering the instance can grant a route into the Abyss, either by its exit data or an in-instance portal.
	 *
	 * @param worldId 副本世界 ID / Instance world id
	 * @param race 玩家种族 / Player race
	 * @return 是否需要欧比斯入场资格 / whether Abyss entry qualification is required
	 */
	public static boolean grantsAbyssAccess(int worldId, Race race) {
		return ABYSS_PORTAL_INSTANCE_WORLD_IDS.contains(worldId) || isAbyssExitInstance(worldId, race);
	}

	private static boolean checkKinahForTransportation(TeleportLocation location, Player player) {
		Storage inventory = player.getInventory();
		int basePrice = (int) (location.getPrice() * 0.8F);
		long transportationPrice = PricesService.getPriceForService(basePrice, player.getRace());
		if (player.getController().isHiPassInEffect()) {
			transportationPrice = 1;
		}

		if (!inventory.tryDecreaseKinah(transportationPrice)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NOT_ENOUGH_KINA(transportationPrice));
			return false;
		}
		return true;
	}

	private static void sendLoc(final Player player, final int mapId, final int instanceId, final float x, final float y, final float z, final byte h, final TeleportAnimation animation) {
		final int targetMapId = resolveInggisonWorldId(mapId);
		boolean isInstance = DataManager.WORLD_MAPS_DATA.getTemplate(targetMapId).isInstance();

		int delay = TELEPORT_DEFAULT_DELAY;

		if (animation.equals(TeleportAnimation.BEAM_ANIMATION)) {
			player.setPortAnimation(2);
			delay = BEAM_DEFAULT_DELAY;
		} else if (animation.equals(TeleportAnimation.JUMP_ANIMATION)) {
			player.setPortAnimation(11);
		}

		PacketSendUtility.sendPacket(player, new SM_TELEPORT_LOC(isInstance, instanceId, targetMapId, x, y, z, h, animation.getStartAnimationId()));
		player.unsetPlayerMode(PlayerMode.RIDE);
		playerTransformation(player);
		instanceTransformation(player);
		archdaevaTransformation(player);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (player.getLifeStats().isAlreadyDead() || !player.isSpawned()) {
				return;
			}
			if (animation.equals(TeleportAnimation.BEAM_ANIMATION)) {
				PacketSendUtility.broadcastPacket(player, new SM_DELETE(player, 2), 50);
			} else if (animation.equals(TeleportAnimation.JUMP_ANIMATION)) {
				PacketSendUtility.broadcastPacket(player, new SM_DELETE(player, 11), 50);
			}
			changePosition(player, targetMapId, instanceId, x, y, z, h, animation);
		}, delay);
	}

	/**
	 * 将玩家传送到世界坐标位置（同图更新或跨图传送）。
	 * Teleports a player to a world position (same-map update or cross-map travel).
	 *
	 * @param player 玩家 / Player
	 * @param pos 目标位置 / Target position
	 */
	public static void teleportTo(Player player, WorldPosition pos) {
		if (player.getWorldId() == pos.getMapId()) {
			player.getPosition().setXYZH(pos.getX(), pos.getY(), pos.getZ(), pos.getHeading());
			// 宠物。 / Pet.

			Pet pet = player.getPet();
			if (pet != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(pet, pos.getMapId(), player.getInstanceId(), pos.getX(), pos.getY(), pos.getZ(), pos.getHeading());
			}

			// 召唤。 / Summon.
			Summon summon = player.getSummon();

			if (summon != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(summon, pos.getMapId(), player.getInstanceId(), pos.getX(), pos.getY(), pos.getZ(), pos.getHeading());
			}
			// 随从 / Minion
			Minion minion = player.getMinion();
			if (minion != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(minion, pos.getMapId(), player.getInstanceId(), pos.getX(), pos.getY(), pos.getZ(), pos.getHeading());
			}

			PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
			PacketSendUtility.sendPacket(player, new SM_CHANNEL_INFO(player.getPosition()));
			player.setPortAnimation(4);
			PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
			player.getController().startProtectionActiveTask();
			PacketSendUtility.sendPacket(player, new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
			// 宠物。 / Pet.
			if (pet != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(pet);
			}

			// 召唤。 / Summon.
			if (summon != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(summon);
			}

			// 随从 / Minion
			if (minion != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(minion);
			}

			player.updateKnownlist();
			player.getKnownList().clear();
			player.getController().updateZone();
			player.getController().updateNearbyQuests();
			GameFeatureServices.disputeLandService().onLogin(player);
			player.getEffectController().updatePlayerEffectIcons();
			ProtectorConquerorService sgs = GameFeatureServices.protectorConquerorService();
			playerTransformation(player);
			instanceTransformation(player);
			archdaevaTransformation(player);
			PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(false, player.getProtectorInfo().getRank()));
			PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(false, player.getConquerorInfo().getRank()));
			if (sgs.isHandledWorld(player.getWorldId()) && (!sgs.isEnemyWorld(player) || sgs.isEnemyWorld(player))) {
				PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(sgs.getWorldProtector(player.getWorldId()).values()));
				PacketSendUtility.sendPacket(player, new SM_CONQUEROR_PROTECTOR(sgs.getWorldConqueror(player.getWorldId()).values()));
			}
		} else if (player.getLifeStats().isAlreadyDead()) {
			teleportDeadTo(player, pos.getMapId(), 1, pos.getX(), pos.getY(), pos.getZ(), pos.getHeading());
		} else {
			teleportTo(player, pos.getMapId(), pos.getX(), pos.getY(), pos.getZ(), pos.getHeading());
		}
	}

	/**
	 * 以死亡状态将玩家传送到指定坐标（不复活）。
	 * Teleports a dead player to coordinates without reviving.
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 * @param instanceId 实例 ID / Instance id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / Heading
	 */
	public static void teleportDeadTo(Player player, int worldId, int instanceId, float x, float y, float z, byte heading) {
		player.getController().onLeaveWorld();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(player);
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, worldId, instanceId, x, y, z, heading);
		PacketSendUtility.sendPacket(player, new SM_CHANNEL_INFO(player.getPosition()));
		PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
		player.setPortAnimation(4);
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
		if (player.isLegionMember()) {
			PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_UPDATE_MEMBER(player, 0, ""));
		}
	}

	/**
	 * 传送到指定世界坐标（使用玩家当前朝向）。
	 * Teleports to world coordinates (uses player heading).
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @return 是否发起传送 / Whether teleport was initiated
	 */
	public static boolean teleportTo(Player player, int worldId, float x, float y, float z) {
		return teleportTo(player, worldId, x, y, z, player.getHeading());
	}

	/**
	 * 传送到指定世界坐标与朝向（默认光束动画）。
	 * Teleports to world coordinates with heading (default beam animation).
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / Heading
	 * @return 是否发起传送 / Whether teleport was initiated
	 */
	public static boolean teleportTo(Player player, int worldId, float x, float y, float z, byte h) {
		int instanceId = 1;
		if (player.getWorldId() == worldId) {
			instanceId = player.getInstanceId();
		}
		return teleportTo(player, worldId, instanceId, x, y, z, h, TeleportAnimation.BEAM_ANIMATION);
	}

	/**
	 * 传送到指定世界坐标与朝向，可指定动画。
	 * Teleports to world coordinates with heading and animation.
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / Heading
	 * @param animation 传送动画 / Teleport animation
	 *
	 * @return 是否发起传送 / Whether teleport was initiated
	 */
	public static boolean teleportTo(Player player, int worldId, float x, float y, float z, byte h, TeleportAnimation animation) {
		int instanceId = 1;
		if (player.getWorldId() == worldId) {
			instanceId = player.getInstanceId();
		}
		return teleportTo(player, worldId, instanceId, x, y, z, h, animation);
	}

	/**
	 * 传送到指定世界/实例坐标（默认光束动画）。
	 * Teleports to world/instance coordinates (default beam animation).
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 * @param instanceId 实例 ID / Instance id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param h 朝向 / Heading
	 * @return 是否发起传送 / Whether teleport was initiated
	 */
	public static boolean teleportTo(Player player, int worldId, int instanceId, float x, float y, float z, byte h) {
		return teleportTo(player, worldId, instanceId, x, y, z, h, TeleportAnimation.BEAM_ANIMATION);
	}

	/**
	 * 传送到指定世界/实例坐标（使用玩家朝向与默认动画）。
	 * Teleports to world/instance coordinates (player heading, default animation).
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 * @param instanceId 实例 ID / Instance id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @return 是否发起传送 / Whether teleport was initiated
	 */
	public static boolean teleportTo(Player player, int worldId, int instanceId, float x, float y, float z) {
		return teleportTo(player, worldId, instanceId, x, y, z, player.getHeading(), TeleportAnimation.BEAM_ANIMATION);
	}

	/**
	 * 完整传送入口：处理决斗中断、离图与动画/无动画换位。
	 * Full teleport entry: ends duel, leave-world and animated/no-anim position change.
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 * @param instanceId 实例 ID / Instance id
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * @param heading 朝向 / Heading
	 * @param animation 传送动画 / Teleport animation
	 *
	 * @return 是否发起传送；死亡中返回 {@code false} / Whether initiated; {@code false} if dead
	 */
	public static boolean teleportTo(final Player player, final int worldId, final int instanceId, final float x, final float y, final float z, final byte heading, TeleportAnimation animation) {
		if (player.getLifeStats().isAlreadyDead()) {
			return false;
		}
		final int targetWorldId = resolveInggisonWorldId(worldId);
		if (isKahrunEntryWorld(targetWorldId) && !player.isGM() && !meetsKahrunEntryRequirement(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return false;
		}
		if (isArchDaevaEntryWorld(targetWorldId) && !player.isGM()
				&& !meetsArchDaevaEntryRequirement(player, targetWorldId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return false;
		}

		if (GameGameplayServices.duelService().isDueling(player.getObjectId())) {
			GameGameplayServices.duelService().loseDuel(player);
		}

		if (player.getWorldId() != targetWorldId) {
			player.getController().onLeaveWorld();
		}

		if (animation.isNoAnimation()) {
			playerTransformation(player);
			instanceTransformation(player);
			archdaevaTransformation(player);
			player.unsetPlayerMode(PlayerMode.RIDE);
			changePosition(player, targetWorldId, instanceId, x, y, z, heading, animation);
		} else {
			sendLoc(player, targetWorldId, instanceId, x, y, z, heading, animation);
		}
		return true;
	}

	private static void changePosition(final Player player, int worldId, int instanceId, float x, float y, float z, byte heading, TeleportAnimation animation) {
		worldId = resolveInggisonWorldId(worldId);
		synchronized (String.valueOf(player.getObjectId()).intern()) {

			if (player.hasStore()) {
				PrivateStoreService.closePrivateStore(player);
			}
			player.getFlyController().endFly(true);

			Pet pet = player.getPet();
			Summon summon = player.getSummon();
			Minion minion = player.getMinion();

			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(player);

			if (pet != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(pet);
			}
			if (summon != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(summon);
			}
			if (minion != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(minion);
			}

			playerTransformation(player);
			instanceTransformation(player);
			archdaevaTransformation(player);
			player.getController().cancelCurrentSkill();

			int currentWorldId = player.getWorldId();
			boolean isInstance = DataManager.WORLD_MAPS_DATA.getTemplate(worldId).isInstance();
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, worldId, instanceId, x, y, z, heading);

			if (pet != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(pet, worldId, instanceId, x, y, z, heading);
			}
			if (summon != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(summon, worldId, instanceId, x, y, z, heading);
			}
			if (minion != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(minion, worldId, instanceId, x, y, z, heading);
			}

			player.setPortAnimation(animation.getEndAnimationId());
			player.getController().startProtectionActiveTask();

			if (currentWorldId == worldId) {
				PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
				PacketSendUtility.sendPacket(player, new SM_STATS_INFO(player));
				player.getController().startProtectionActiveTask();
				PacketSendUtility.sendPacket(player, new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));

				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(player);

				player.getEffectController().updatePlayerEffectIcons();
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
				GameFeatureServices.disputeLandService().onLogin(player);

				playerTransformation(player);
				instanceTransformation(player);
				archdaevaTransformation(player);

				if (pet != null) {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(pet);
					player.setPortAnimation(4);
				}
				if (summon != null) {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(summon);
					player.setPortAnimation(4);
				}
				if (minion != null) {
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().spawn(minion);
					player.setPortAnimation(4);
				}

				player.getKnownList().clear();
				player.updateKnownlist();
				if (player.isUseRobot() || player.getRobotId() != 0) {
					PacketSendUtility.sendPacket(player, new SM_USE_ROBOT(player, getRobotInfo(player).getRobotId()));
				}
			} else {
				PacketSendUtility.sendPacket(player, new SM_CHANNEL_INFO(player.getPosition()));
				PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
				playerTransformation(player);
				instanceTransformation(player);
				archdaevaTransformation(player);
				if (player.isUseRobot() || player.getRobotId() != 0) {
					GameThreadPoolServices.threadPoolManager().schedule(() -> PacketSendUtility.sendPacket(player, new SM_USE_ROBOT(player, getRobotInfo(player).getRobotId())), 3000);
				}
			}

			if (player.isLegionMember()) {
				PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_UPDATE_MEMBER(player, 0, ""));
			}

			sendWorldSwitchMessage(player, currentWorldId, worldId, isInstance);
		}
	}

	/**
	 * 根据主手武器皮肤解析机器人信息。
	 * Resolves robot info from the main-hand weapon skin.
	 *
	 * @param player 玩家 / Player
	 * @return 机器人信息 / Robot info
	 */
	public static RobotInfo getRobotInfo(Player player) {
		ItemTemplate template = player.getEquipment().getMainHandWeapon().getItemSkinTemplate();
		return DataManager.ROBOT_DATA.getRobotInfo(template.getRobotId());
	}

	private static void sendWorldSwitchMessage(Player player, int oldWorld, int newWorld, boolean enteredInstance) {
		if ((enteredInstance) && (oldWorld != newWorld) && (!WorldMapType.getWorld(newWorld).isPersonal())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_INSTANCE_DUNGEON_OPENED_FOR_SELF(newWorld));
		}

		playerTransformation(player);
		instanceTransformation(player);
		archdaevaTransformation(player);
	}

	/**
	 * 向玩家打开传送员地图界面。
	 * Opens the teleporter map UI for the player.
	 *
	 * @param player 玩家 / Player
	 * @param targetObjectId 目标对象 ID / Target object id
	 * @param npcId 传送员 NPC ID / Teleporter NPC id
	 */
	public static void showMap(Player player, int targetObjectId, int npcId) {
		if (player.isInFlyingState()) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_USE_AIRPORT_WHEN_FLYING);
			return;
		}

		Npc object = (Npc) com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(targetObjectId);

		if (player.isEnemy(object)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_WRONG_NPC);
			return;
		}

		PacketSendUtility.sendPacket(player, new SM_TELEPORT_MAP(player, targetObjectId, getTeleporterTemplate(npcId)));
	}

	/**
	 * 按 NPC ID 获取传送员模板。
	 * Returns teleporter template by NPC id.
	 *
	 * @param npcId NPC ID / NPC id
	 * @return 传送员模板 / Teleporter template
	 */
	public static TeleporterTemplate getTeleporterTemplate(int npcId) {
		return DataManager.TELEPORTER_DATA.getTeleporterTemplateByNpcId(npcId);
	}

	/**
	 * 将玩家传送到 Kisk（复活石）位置。
	 * Teleports the player to a Kisk location.
	 *
	 * @param player 玩家 / Player
	 * @param kisk Kisk 位置 / Kisk position
	 */
	public static void moveToKiskLocation(Player player, WorldPosition kisk) {
		int mapId = kisk.getMapId();
		float x = kisk.getX();
		float y = kisk.getY();
		float z = kisk.getZ();
		byte heading = kisk.getHeading();
		teleportTo(player, mapId, x, y, z, heading, TeleportAnimation.NO_ANIMATION);
	}

	/**
	 * 按种族将玩家送入监狱地图。
	 * Teleports the player into the race prison map.
	 *
	 * @param player 玩家 / Player
	 */
	public static void teleportToPrison(Player player) {
		if (player.getRace() == Race.ELYOS) {
			teleportTo(player, WorldMapType.DE_PRISON.getId(), 275.0f, 239.0f, 49.0f);
		} else if (player.getRace() == Race.ASMODIANS) {
			teleportTo(player, WorldMapType.DF_PRISON.getId(), 275.0f, 239.0f, 49.0f);
		}
	}

	/**
	 * 将玩家传送到当前世界中指定 NPC 的生成点。
	 * Teleports the player to the spawn of the given NPC in the current world.
	 *
	 * @param player 玩家 / Player
	 * @param npcId NPC ID / NPC id
	 */
	public static void teleportToNpc(Player player, int npcId) {
		int worldId = player.getWorldId();
		SpawnSearchResult searchResult = DataManager.SPAWNS_DATA2.getFirstSpawnByNpcId(worldId, npcId);
		if (searchResult == null) {
			log.warn(I18n.get("log.02aebd9dc714", npcId));
			return;
		}
		teleportToNpc(player, searchResult);
	}

	/** 将 GM 传送到存活 NPC 当前坐标，避免移动 NPC 的静态刷怪点不可达。
	 * Teleports a GM to the live NPC's current position, since moving NPCs' static spawn spots may be unreachable. */
	public static void teleportToNpc(Player player, Npc npc) {
		if (player == null || npc == null || !npc.isSpawned()
				|| (npc.getLifeStats() != null && npc.getLifeStats().isAlreadyDead())) {
			return;
		}
		teleportTo(player, npc.getWorldId(), npc.getInstanceId(), npc.getX(), npc.getY(), npc.getZ(),
				npc.getHeading());
	}

	/** 将玩家传送到已解析的 NPC 刷新点，确保地图标记与 GM 传送目标一致。
	 * Teleports the player to a resolved NPC spawn spot, keeping map markers and GM teleport targets consistent. */
	public static void teleportToNpc(Player player, SpawnSearchResult searchResult) {
		if (player == null || searchResult == null || searchResult.getSpot() == null) {
			return;
		}

		SpawnSpotTemplate spot = searchResult.getSpot();
		WorldMapTemplate worldTemplate = DataManager.WORLD_MAPS_DATA.getTemplate(searchResult.getWorldId());
		WorldMapInstance newInstance = null;

		if (worldTemplate.isInstance()) {
			newInstance = InstanceService.getNextAvailableInstance(searchResult.getWorldId());
		}

		if (newInstance != null) {
			InstanceService.registerPlayerWithInstance(newInstance, player);
			teleportTo(player, searchResult.getWorldId(), newInstance.getInstanceId(), spot.getX(), spot.getY(), spot.getZ());
		} else {
			teleportTo(player, searchResult.getWorldId(), spot.getX(), spot.getY(), spot.getZ());
		}
	}

	/**
	 * 向客户端发送绑定点（回城点）信息。
	 * Sends bind-point info to the client.
	 *
	 * @param player 玩家 / Player
	 */
	public static void sendSetBindPoint(Player player) {
		int worldId;
		float x, y, z;
		if (player.getBindPoint() != null) {
			BindPointPosition bplist = player.getBindPoint();
			worldId = bplist.getMapId();
			x = bplist.getX();
			y = bplist.getY();
			z = bplist.getZ();
		} else {
			PlayerInitialData.LocationData locationData = DataManager.PLAYER_INITIAL_DATA.getSpawnLocation(player.getRace());
			worldId = locationData.getMapId();
			x = locationData.getX();
			y = locationData.getY();
			z = locationData.getZ();
		}
		PacketSendUtility.sendPacket(player, new SM_BIND_POINT_INFO(worldId, x, y, z, player));
	}

	/**
	 * 将玩家送回绑定点；可选是否走完整传送流程。
	 * Moves the player to bind point; optionally via full teleport.
	 *
	 * @param player 玩家 / Player
	 * @param useTeleport {@code true} 使用传送路径 / {@code true} to use teleport path
	 */
	public static void moveToBindLocation(Player player, boolean useTeleport) {
		float x, y, z;
		int worldId;
		byte h = 0;

		if (player.getBindPoint() != null) {
			BindPointPosition bplist = player.getBindPoint();
			worldId = bplist.getMapId();
			x = bplist.getX();
			y = bplist.getY();
			z = bplist.getZ();
			h = bplist.getHeading();
		} else {
			PlayerInitialData.LocationData locationData = DataManager.PLAYER_INITIAL_DATA.getSpawnLocation(player.getRace());
			worldId = locationData.getMapId();
			x = locationData.getX();
			y = locationData.getY();
			z = locationData.getZ();
		}

		InstanceService.onLeaveInstance(player);

		if (useTeleport) {
			teleportTo(player, worldId, x, y, z, h, TeleportAnimation.NO_ANIMATION);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, worldId, 1, x, y, z, h);
		}
	}

	/**
	 * 将玩家传送到副本出口；无配置时回绑定点。
	 * Teleports the player to instance exit; falls back to bind point.
	 *
	 * @param player 玩家 / Player
	 * @param worldId 副本世界 ID / Instance world id
	 * @param race 玩家种族 / Player race
	 */
	public static void moveToInstanceExit(Player player, int worldId, Race race) {
		player.getController().cancelCurrentSkill();
		InstanceExit instanceExit = getInstanceExit(worldId, race);
		if (instanceExit == null) {
			log.warn(I18n.get("log.7a71dbd66b80", race, worldId));
			moveToBindLocation(player, true);
			return;
		}

		// 未完成欧比斯入场任务时，不得借副本出口直接进入欧比斯。
		// Without Abyss entry qualification, instance exits must not grant access to the Abyss.
		if (isAbyssEntryWorld(instanceExit.getExitWorld()) && !meetsAbyssEntryRequirement(player)) {
			moveToBindLocation(player, true);
			return;
		}

		if (InstanceService.isInstanceExist(instanceExit.getExitWorld(), 1)) {
			teleportTo(player, instanceExit.getExitWorld(), instanceExit.getX(), instanceExit.getY(), instanceExit.getZ(), instanceExit.getH());
		} else {
			moveToBindLocation(player, true);
		}
	}

	/**
	 * 登出时处理敌对地图（阿斯泰拉/诺斯珀德）的位置修正。
	 * On logout, corrects position when on the opposite-race map (Iluma/Norsvold).
	 *
	 * @param player 玩家 / Player
	 */
	public static void onLogOutOppositeMap(Player player) {
		switch (player.getWorldId()) {
		case 210100000: // Iluma.
			if (player.getCommonData().getRace() == Race.ASMODIANS) {
				TeleportService2.teleportTo(player, 220110000, 1813.9795f, 1982.6705f, 199.1976f, (byte) 52);
			}
			break;
		case 220110000: // Norsvold.
			if (player.getCommonData().getRace() == Race.ELYOS) {
				TeleportService2.teleportTo(player, 210100000, 1417.6694f, 1282.3623f, 336.125f, (byte) 8);
			}
			break;
		}
	}

	/**
	 * 获取副本出口模板。
	 * Returns instance-exit template.
	 *
	 * @param worldId 世界 ID / World id
	 * @param race 阵营 / Race
	 * @return 出口模板 / exit template
	 */
	public static InstanceExit getInstanceExit(int worldId, Race race) {
		return DataManager.INSTANCE_EXIT_DATA.getInstanceExit(worldId, race);
	}

	/**
	 * 获取副本复活起点模板。
	 * Returns instance revive start-point template.
	 *
	 * @param worldId 世界 ID / World id
	 * @return 复活起点 / revive start point
	 */
	public static InstanceReviveStartPoints getReviveInstanceStartPoints(int worldId) {
		return DataManager.REVIVE_INSTANCE_START_POINTS.getReviveStartPoint(worldId);
	}

	/**
	 * 获取世界复活起点模板。
	 * Returns world revive start-point template.
	 *
	 * @param worldId 世界 ID / World id
	 * @param race 阵营 / Race
	 * @param level 等级 / Level
	 * @return 复活起点 / revive start point
	 */
	public static WorldReviveStartPoints getReviveWorldStartPoints(int worldId, Race race, int level) {
		return DataManager.REVIVE_WORLD_START_POINTS.getReviveStartPoint(worldId, race, level);
	}

	/**
	 * 使用传送卷轴按门户名传送到目标世界。
	 * Uses a portal scroll to teleport by portal name into a world.
	 *
	 * @param player 玩家 / Player
	 * @param portalName 门户名称 / Portal name
	 * @param worldId 目标世界 ID / Target world id
	 */
	public static void useTeleportScroll(Player player, String portalName, int worldId) {
		PortalScroll template = DataManager.PORTAL2_DATA.getPortalScroll(portalName);
		if (template == null) {
			log.warn(I18n.get("log.f31a379211ec", portalName, worldId));
			return;
		}

		Race playerRace = player.getRace();
		PortalPath portalPath = template.getPortalPath();

		if (portalPath == null) {
			log.warn(I18n.get("log.b99e4df2cc33", playerRace, portalName, worldId));
			return;
		}

		PortalLoc loc = DataManager.PORTAL_LOC_DATA.getPortalLoc(portalPath.getLocId());

		if (loc == null) {
			log.warn(I18n.get("log.e07f399d3e8d", portalPath.getLocId()));
			return;
		}

		if (isAbyssEntryWorld(worldId) && !meetsAbyssEntryRequirement(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CANNOT_TELEPORT_TO_ABYSS);
			return;
		}
		if (isBalaureaEntryWorld(worldId) && !meetsBalaureaEntryRequirement(player, worldId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}
		if (isKahrunEntryWorld(worldId) && !meetsKahrunEntryRequirement(player)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}
		if (isArchDaevaEntryWorld(worldId) && !meetsArchDaevaEntryRequirement(player, worldId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CANNOT_MOVE_TO_AIRPORT_NEED_FINISH_QUEST);
			return;
		}

		teleportTo(player, worldId, loc.getX(), loc.getY(), loc.getZ(), player.getHeading(), TeleportAnimation.BEAM_ANIMATION);
	}

	/**
	 * 将玩家置于世界复活起点（无完整传送动画）。
	 * Places the player at the world revive start point (no full teleport anim).
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 */
	public static void teleportWorldStartPoint(Player player, int worldId) {
		player.getController().onLeaveWorld();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(player);
		WorldReviveStartPoints startPoint = getReviveWorldStartPoints(worldId, player.getRace(), player.getLevel());

		if (startPoint != null) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, startPoint.getReviveWorld(), 0, startPoint.getX(), startPoint.getY(), startPoint.getZ(), startPoint.getH());
		} else {
			moveToBindLocation(player, false);
		}

		PacketSendUtility.sendPacket(player, new SM_CHANNEL_INFO(player.getPosition()));
		PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
		player.setPortAnimation(4);
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));

		if (player.isLegionMember()) {
			PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_UPDATE_MEMBER(player, 0, ""));
		}
	}

	/**
	 * 将玩家置于副本复活起点。
	 * Places the player at the instance revive start point.
	 *
	 * @param player 玩家 / Player
	 * @param worldId 世界 ID / World id
	 */
	public static void teleportInstanceStartPoint(Player player, int worldId) {
		player.getController().onLeaveWorld();
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(player);
		InstanceReviveStartPoints revivePoint = getReviveInstanceStartPoints(worldId);

		if (revivePoint != null) {
			TeleportService2.teleportTo(player, worldId, worldId, revivePoint.getX(), revivePoint.getY(), revivePoint.getY(), (byte) revivePoint.getY());
		} else {
			moveToBindLocation(player, false);
		}

		PacketSendUtility.sendPacket(player, new SM_CHANNEL_INFO(player.getPosition()));
		PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
		player.setPortAnimation(4);
		PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));

		if (player.isLegionMember()) {
			PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_UPDATE_MEMBER(player, 0, ""));
		}
	}

	/**
	 * 切换玩家所在频道（同图不同实例线）。
	 * Changes the player's channel (same map, different instance line).
	 *
	 * @param player 玩家 / Player
	 * @param channel 频道序号（0 起） / Channel index (0-based)
	 */
	public static void changeChannel(Player player, int channel) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(player);
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, player.getWorldId(), channel + 1, player.getX(), player.getY(), player.getZ(), player.getHeading());
		player.getController().startProtectionActiveTask();
		PacketSendUtility.sendPacket(player, new SM_CHANNEL_INFO(player.getPosition()));
		PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
		playerTransformation(player);
		instanceTransformation(player);
		archdaevaTransformation(player);
	}

	/**
	 * 处理 A-Station 跨服进出位置同步。
	 * Handles A-Station cross-server enter/leave position sync.
	 *
	 * @param player 玩家 / Player
	 * @param serverId 对方服务器 ID / Peer server id
	 * @param back {@code true} 返回本服 / {@code true} return to home server
	 */
	public static void moveAStation(Player player, int serverId, boolean back) {
		if (back) {
			playerTransformation(player);
			instanceTransformation(player);
			archdaevaTransformation(player);
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(player);
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, player.getWorldId(), player.getX(), player.getY(), player.getZ(), player.getHeading());
			player.getController().startProtectionActiveTask();
			player.A_STATION_TYPE = 0;
			PacketSendUtility.sendPacket(player, new SM_A_STATION_MOVE(NetworkConfig.GAMESERVER_ID, serverId, player.getWorldId()));
			PacketSendUtility.sendPacket(player, new SM_A_STATION(NetworkConfig.GAMESERVER_ID, serverId, false));
			PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
			GameFeatureServices.aStationService().checkAStationMove(player, player.getPlayerAccount().getId(), true);
		} else {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(player);
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, player.getWorldId(), player.getX(), player.getY(), player.getZ(), player.getHeading());
			player.getController().startProtectionActiveTask();
			player.A_STATION_TYPE = 1;
			PacketSendUtility.sendPacket(player, new SM_A_STATION_MOVE(serverId, NetworkConfig.GAMESERVER_ID, player.getWorldId()));
			PacketSendUtility.sendPacket(player, new SM_A_STATION(serverId, NetworkConfig.GAMESERVER_ID, false));
			PacketSendUtility.sendPacket(player, new SM_PLAYER_SPAWN(player));
			playerTransformation(player);
			instanceTransformation(player);
			archdaevaTransformation(player);
			GameFeatureServices.aStationService().checkAStationMove(player, player.getPlayerAccount().getId(), false);
		}
	}

	/**
	 * 从数据库加载并同步玩家变形状态到客户端。
	 * Loads and syncs player transformation state to the client.
	 *
	 * @param player 玩家 / Player
	 */
	public static void playerTransformation(Player player) {
		TransformPanelSync.loadAndSyncTransformation(player);
	}

	/**
	 * 同步大天使变形技能面板（传送后保持效果关联面板）。
	 * Syncs Archdaeva transform skill panels (keeps effect-linked panels after teleport).
	 * <p>
	 * Archdaeva Transformation 5.1: If a player is under one of the following effects, and uses a
	 * "Teleport/Fly/Hotspot/Return Scroll" or admin command "goto/movetoplayer/movetonpc",
	 * the skill panel linked to this effect must not disappear.
	 *
	 * @param player 玩家 / Player
	 */
	public static void archdaevaTransformation(Player player) {
		TransformPanelSync.syncArchdaevaPanels(player);
	}

	/**
	 * 同步副本/活动变形技能面板（传送后保持效果关联面板）。
	 * Syncs instance/event transform skill panels (keeps effect-linked panels after teleport).
	 * <p>
	 * Instance + Event Transformation: If a player is under one of the following effects, and uses a
	 * "Teleport/Fly/Hotspot/Return Scroll" or admin command "goto/movetoplayer/movetonpc",
	 * the skill panel linked to this effect must not disappear.
	 *
	 * @param player 玩家 / Player
	 */
	public static void instanceTransformation(Player player) {
		TransformPanelSync.syncInstanceEventPanels(player);
	}

	/**
	 * 按种族将玩家传送到主城（无特殊动画）。
	 * Teleports the player to capital city by race (no special animation).
	 *
	 * @param player 玩家 / Player
	 */
	public static void teleportToCapital(Player player) {
		switch (player.getRace()) {
		case ELYOS:
			TeleportService2.teleportTo(player, WorldMapType.SANCTUM.getId(), 1322, 1511, 568);
			break;
		case ASMODIANS:
			TeleportService2.teleportTo(player, WorldMapType.PANDAEMONIUM.getId(), 1679, 1400, 195);
			break;
		default:
			break;
		}
	}

	/**
	 * 按种族将玩家传送到主城（跳跃动画）。
	 * Teleports the player to capital city by race (jump animation).
	 *
	 * @param player 玩家 / Player
	 */
	public static void teleportToCapital2(Player player) {
		switch (player.getRace()) {
		case ELYOS:
			teleportTo(player, WorldMapType.SANCTUM.getId(), 1322, 1511, 568, player.getHeading(), TeleportAnimation.JUMP_ANIMATION);
			break;
		case ASMODIANS:
			teleportTo(player, WorldMapType.PANDAEMONIUM.getId(), 1679, 1400, 195, player.getHeading(), TeleportAnimation.JUMP_ANIMATION);
			break;
		default:
			break;
		}
	}
}
