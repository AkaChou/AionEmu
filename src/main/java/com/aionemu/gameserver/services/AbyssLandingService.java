package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.AbyssLandingConfig;
import com.aionemu.gameserver.dao.AbyssLandingDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.landing.LandingPointsEnum;
import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.landingspawns.LandingSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_LANDING;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_LANDING_LEVEL;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abysslandingservice.AbyssLanding;
import com.aionemu.gameserver.services.abysslandingservice.Landing;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 欧比斯登陆点（Abyss Landing）服务：救赎/先驱登陆点等级、积分、刷怪与广播。
 * Abyss Landing service: Redemption/Harbinger landing levels, points, spawns and broadcasts.
 */
@Slf4j

public class AbyssLandingService {
	private static volatile ObjectProvider<AbyssLandingService> instanceProvider;
	private static Map<Integer, LandingLocation> abyssLanding;
	private final ConcurrentMap<Integer, Landing<?>> activeLanding = new ConcurrentHashMap<Integer, Landing<?>>();

	/**
	 * 从静态数据与数据库加载登陆点并全部启动。
	 * Load landing locations from static data and DB, then start all of them.
	 */
	public void initLandingLocations() {
		abyssLanding = DataManager.LANDING_LOCATION_DATA.getLandingLocations();
		DAOManager.getDAO(AbyssLandingDAO.class).loadLandingLocations(abyssLanding);
		for (LandingLocation loc : getLandingLocations().values()) {
			startLanding(loc.getId());
		}
		log.info(I18n.get("log.c7417b6d4d56", abyssLanding.size()));
	}

	/**
	 * 启动指定登陆点（按当前等级）。
	 * Start the landing at the given id using its current level.
	 *
	 * @param id 登陆点 ID / landing location id
	 */
	public void startLanding(final int id) {
		Landing<?> land = new AbyssLanding(abyssLanding.get(id));
		if (activeLanding.putIfAbsent(id, land) != null) {
			return;
		}
		land.start(getLandingLocation(id).getLevel());
	}

	/**
	 * 停止指定登陆点。
	 * Stop the landing at the given id.
	 *
	 * @param id 登陆点 ID / landing location id
	 */
	public void stopLanding(int id) {
		Landing<?> landing = activeLanding.remove(id);
		if (landing == null) {
			return;
		}
		landing.stop();
	}

	/**
	 * 按状态刷出登陆点相关 NPC。
	 * Spawn landing NPCs for the given location and state.
	 *
	 * landing location
	 * spawn state
	 */
	public static void spawn(LandingLocation loc, LandingStateType estate) {
		if (estate.equals(estate)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getLandingSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				LandingSpawnTemplate landingtTemplate = (LandingSpawnTemplate) st;
				if (landingtTemplate.getEStateType().equals(estate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(landingtTemplate, 1));
				}
			}
		}
	}

	/**
	 * 清除指定登陆点已刷出的 NPC。
	 * Despawn NPCs at the given landing location.
	 *
	 * landing location
	 */
	public static void despawn(LandingLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<VisibleObject>(loc.getSpawned())) {
			Npc spawned = (Npc) obj;
			spawned.setDespawnDelayed(true);
			if (spawned.getAggroList().getList().isEmpty()) {
				spawned.getController().cancelTask(TaskId.RESPAWN);
				obj.getController().onDelete();
			}
		}
		loc.getSpawned().clear();
	}

	/**
	 * 更新救赎登陆点积分（胜负加减），并检查是否升级。
	 * Update Redemption landing points (add/subtract by outcome) and check for level-up.
	 *
	 * @param points 积分变化量 / points delta
	 * @param type 积分类型 / points category
	 * @param win 是否胜利加分 / true to gain points
	 */
	public void updateRedemptionLanding(int points, LandingPointsEnum type, boolean win) {
		LandingLocation loc = redemptionLanding();
		if (win) {
			switch (type) {
			case BASE:
				loc.setBasePoints(loc.getBasePoints() + points);
				break;
			case SIEGE:
				loc.setSiegePoints(loc.getSiegePoints() + points);
				break;
			case COMMANDER:
				loc.setCommanderPoints(loc.getCommanderPoints() + points);
				break;
			case ARTIFACT:
				loc.setArtifactPoints(loc.getArtifactPoints() + points);
				break;
			case QUEST:
				loc.setQuestPoints(loc.getQuestPoints() + (points * AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE));
				break;
			case MONUMENT:
				loc.setMonumentsPoints(loc.getMonumentsPoints() + points);
				break;
			case FACILITY:
				loc.setFacilityPoints(loc.getFacilityPoints() + points);
				break;
			}
		} else {
			switch (type) {
			case BASE:
				if (loc.getBasePoints() < points) {
					return;
				} else {
					loc.setBasePoints(loc.getBasePoints() - points);
				}
				break;
			case SIEGE:
				if (loc.getSiegePoints() < points) {
					return;
				} else {
					loc.setSiegePoints(loc.getSiegePoints() - points);
				}
				break;
			case COMMANDER:
				if (loc.getCommanderPoints() < points) {
					return;
				} else {
					loc.setCommanderPoints(loc.getCommanderPoints() - points);
				}
				break;
			case ARTIFACT:
				if (loc.getArtifactPoints() < points) {
					return;
				} else {
					loc.setArtifactPoints(loc.getArtifactPoints() - points);
				}
				break;
			case QUEST:
				if (loc.getQuestPoints() < (points * AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE)) {
					return;
				} else {
					loc.setQuestPoints(loc.getQuestPoints() - (points * AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE));
				}
				break;
			case MONUMENT:
				if (loc.getMonumentsPoints() < points) {
					return;
				} else {
					loc.setMonumentsPoints(loc.getMonumentsPoints() - points);
				}
				break;
			case FACILITY:
				if (loc.getFacilityPoints() < points) {
					return;
				} else {
					loc.setFacilityPoints(loc.getFacilityPoints() - points);
				}
				break;
			}
		}
		int totalScore = loc.getArtifactPoints() + loc.getCommanderPoints() + loc.getFacilityPoints()
				+ loc.getBasePoints() + loc.getMonumentsPoints() + loc.getQuestPoints() + loc.getSiegePoints();
		loc.setPoints(totalScore);
		if (win) {
			checkRedemptionLanding(totalScore, true);
		} else {
			checkRedemptionLanding(totalScore, false);
		}
		onUpdate();
	}

	/**
	 * 更新先驱登陆点积分（胜负加减），并检查是否升级。
	 * Update Harbinger landing points (add/subtract by outcome) and check for level-up.
	 *
	 * @param points 积分变化量 / points delta
	 * @param type 积分类型 / points category
	 * @param win 是否胜利加分 / true to gain points
	 */
	public void updateHarbingerLanding(int points, LandingPointsEnum type, boolean win) {
		LandingLocation loc = harbingerLanding();
		if (win) {
			switch (type) {
			case BASE:
				loc.setBasePoints(loc.getBasePoints() + points);
				break;
			case SIEGE:
				loc.setSiegePoints(loc.getSiegePoints() + points);
				break;
			case COMMANDER:
				loc.setCommanderPoints(loc.getCommanderPoints() + points);
				break;
			case ARTIFACT:
				loc.setArtifactPoints(loc.getArtifactPoints() + points);
				break;
			case QUEST:
				loc.setQuestPoints(loc.getQuestPoints() + (points * AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE));
				break;
			case MONUMENT:
				loc.setMonumentsPoints(loc.getMonumentsPoints() + points);
				break;
			case FACILITY:
				loc.setFacilityPoints(loc.getFacilityPoints() + points);
				break;
			}
		} else {
			switch (type) {
			case BASE:
				if (loc.getBasePoints() < points) {
					return;
				} else {
					loc.setBasePoints(loc.getBasePoints() - points);
				}
				break;
			case SIEGE:
				if (loc.getSiegePoints() < points) {
					return;
				} else {
					loc.setSiegePoints(loc.getSiegePoints() - points);
				}
				break;
			case COMMANDER:
				if (loc.getCommanderPoints() < points) {
					return;
				} else {
					loc.setCommanderPoints(loc.getCommanderPoints() - points);
				}
				break;
			case ARTIFACT:
				if (loc.getArtifactPoints() < points) {
					return;
				} else {
					loc.setArtifactPoints(loc.getArtifactPoints() - points);
				}
				break;
			case QUEST:
				if (loc.getQuestPoints() < (points * AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE)) {
					return;
				} else {
					loc.setQuestPoints(loc.getQuestPoints() - (points * AbyssLandingConfig.ABYSS_LANDING_QUEST_RATE));
				}
				break;
			case MONUMENT:
				if (loc.getMonumentsPoints() < points) {
					return;
				} else {
					loc.setMonumentsPoints(loc.getMonumentsPoints() - points);
				}
				break;
			case FACILITY:
				if (loc.getFacilityPoints() < points) {
					return;
				} else {
					loc.setFacilityPoints(loc.getFacilityPoints() - points);
				}
				break;
			}
		}
		int totalScore = loc.getArtifactPoints() + loc.getCommanderPoints() + loc.getFacilityPoints()
				+ loc.getBasePoints() + loc.getMonumentsPoints() + loc.getQuestPoints() + loc.getSiegePoints();
		loc.setPoints(totalScore);
		if (win) {
			checkHarbingerLanding(totalScore, true);
		} else {
			checkHarbingerLanding(totalScore, false);
		}
		onUpdate();
	}

	/**
	 * 向全服广播登陆点积分获取/失去公告。
	 * Broadcast a landing-points gain/loss announcement to all players.
	 *
	 * @param pl 触发玩家 / source player
	 * @param race 阵营描述 ID / race description id
	 * @param name 目标名称描述 ID / target name description id
	 * points amount
	 */
	public void AnnounceToPoints(final Player pl, final DescriptionId race, final DescriptionId name, final int points,
			final LandingPointsEnum type) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				switch (type) {
				case SIEGE:
					// %0 占领了 %0，登陆点已增强。 / %0 has occupied %0 and the Landing is now enhanced.
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_OCCUPY(race, name));
					break;
				case BASE:
					// %0 已占领 %1 基地，登陆点已增强。 / %0 has occupied %1 Base and the Landing is now enhanced.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
							.STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_OCCUPY_BASECAMP(race, name.toString()));
					break;
				case QUEST:
					// 已完成任务为登陆点贡献了 %0 点。 / Completed quest has contributed %0 points to the Landing.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_BUILDUP_POINT_QUEST_GAIN(points));
					// %0 已完成的任务增强了登陆点。 / %0's completed quest has enhanced the Landing.
					PacketSendUtility.sendPacket(player,
							SM_SYSTEM_MESSAGE.STR_MSG_BUILDUP_NOTICE_CONTRIBUTE_USER_QUEST(pl.getName()));
					break;
				}
			}
		});
	}

	/**
	 * 检查救赎登陆点是否因积分变化而升级/降级。
	 * Check whether Redemption landing should level up/down after a points change.
	 *
	 * current points
	 * @param gain 是否为获得积分 / true if points were gained
	 */
	public void checkRedemptionLanding(int points, boolean gain) {
		int level = 0;
		if (points >= 0 && points <= 199999) {
			level = 1;
		} else if (points >= 200000 && points <= 299999) {
			level = 2;
		} else if (points >= 300000 && points <= 399999) {
			level = 3;
		} else if (points >= 400000 && points <= 499999) {
			level = 4;
		} else if (points >= 500000 && points <= 599999) {
			level = 5;
		} else if (points >= 600000 && points <= 699999) {
			level = 6;
		} else if (points >= 700000 && points <= 799999) {
			level = 7;
		} else if (points >= 800000) {
			level = 8;
		}
		if (gain && level != redemptionLanding().getLevel()) {
			levelUpRedemptionLanding(level);
		}
		if (!gain && level != redemptionLanding().getLevel()) {
			onRedemptionLandingLevelDown(level);
		}
	}

	/**
	 * 检查先驱登陆点是否因积分变化而升级/降级。
	 * Check whether Harbinger landing should level up/down after a points change.
	 *
	 * current points
	 * @param gain 是否为获得积分 / true if points were gained
	 */
	public void checkHarbingerLanding(int points, boolean gain) {
		int level = 0;
		if (points >= 0 && points <= 199999) {
			level = 1;
		} else if (points >= 200000 && points <= 299999) {
			level = 2;
		} else if (points >= 300000 && points <= 399999) {
			level = 3;
		} else if (points >= 400000 && points <= 499999) {
			level = 4;
		} else if (points >= 500000 && points <= 599999) {
			level = 5;
		} else if (points >= 600000 && points <= 699999) {
			level = 6;
		} else if (points >= 700000 && points <= 799999) {
			level = 7;
		} else if (points >= 800000) {
			level = 8;
		}
		if (gain && level != harbingerLanding().getLevel()) {
			levelUpHarbingerLanding(level);
		}
		if (!gain && level != harbingerLanding().getLevel()) {
			onHarbingerLandingLevelDown(level);
		}
	}

	/**
	 * 救赎登陆点升级处理（刷怪、广播、发包）。
	 * Handle Redemption landing level-up (spawn, broadcast, packets).
	 *
	 * new level
	 */
	public void levelUpRedemptionLanding(int level) {
		redemptionLanding().setLevel(level);
		stopLanding(redemptionLanding().getId());
		startLanding(redemptionLanding().getId());
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 登陆点升级。 / Landing Level Up.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_UP_LIGHT);
				PacketSendUtility.sendPacket(player,
						new SM_ABYSS_LANDING_LEVEL(0, redemptionLanding().getLevel(), redemptionLanding().getLevel()));
			}
		});
	}

	/**
	 * 先驱登陆点升级处理（刷怪、广播、发包）。
	 * Handle Harbinger landing level-up (spawn, broadcast, packets).
	 *
	 * new level
	 */
	public void levelUpHarbingerLanding(int level) {
		harbingerLanding().setLevel(level);
		stopLanding(harbingerLanding().getId());
		startLanding(harbingerLanding().getId());
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 登陆点升级。 / Landing Level Up.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_UP_DARK);
				PacketSendUtility.sendPacket(player,
						new SM_ABYSS_LANDING_LEVEL(1, harbingerLanding().getLevel(), harbingerLanding().getLevel()));
			}
		});
	}

	/**
	 * 先驱登陆点降级处理。
	 * Handle Harbinger landing level-down.
	 *
	 * new level
	 */
	public void onHarbingerLandingLevelDown(int level) {
		harbingerLanding().setLevel(level);
		stopLanding(harbingerLanding().getId());
		startLanding(harbingerLanding().getId());
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 登陆点削弱。 / Landing Weakened.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_DOWN);
				PacketSendUtility.sendPacket(player,
						new SM_ABYSS_LANDING_LEVEL(1, harbingerLanding().getLevel(), harbingerLanding().getLevel()));
			}
		});
	}

	/**
	 * 救赎登陆点降级处理。
	 * Handle Redemption landing level-down.
	 *
	 * new level
	 */
	public void onRedemptionLandingLevelDown(int level) {
		redemptionLanding().setLevel(level);
		stopLanding(redemptionLanding().getId());
		startLanding(redemptionLanding().getId());
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 登陆点削弱。 / Landing Weakened.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ABYSS_OP_LEVEL_DOWN);
				PacketSendUtility.sendPacket(player,
						new SM_ABYSS_LANDING_LEVEL(0, redemptionLanding().getLevel(), redemptionLanding().getLevel()));
			}
		});
	}

		/**
	 * 纪念碑相关奖励积分入账。
	 * Apply monument reward points for the given race/location.
	 *
	 * 阵营 / race
	 * @param id 纪念碑/地点 ID / monument or location id
	 * points
	 */
	public void onRewardMonuments(Race race, int id, int points) {
		LandingSpecialLocation lsl = GameLocationBootstrapServices.abyssLandingSpecialService().getLandingSpecialLocation(id);
		if (race == Race.ASMODIANS) {
			updateHarbingerLanding(points, LandingPointsEnum.MONUMENT, true);
			GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(id);
		} else {
			updateRedemptionLanding(points, LandingPointsEnum.MONUMENT, true);
			GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(id);
		}
		lsl.setType(LandingSpecialStateType.ACTIVE);
		AbyssLandingSpecialService.onSave(lsl);
	}

	/**
	 * 纪念碑被摧毁时的积分处理。
	 * Apply monument-death point changes for the given race/location.
	 *
	 * 阵营 / race
	 * @param id 纪念碑/地点 ID / monument or location id
	 * points
	 */
	public void onDieMonuments(Race race, int id, int points) {
		LandingSpecialLocation lsl = GameLocationBootstrapServices.abyssLandingSpecialService().getLandingSpecialLocation(id);
		if (race == Race.ELYOS) {
			updateRedemptionLanding(points, LandingPointsEnum.MONUMENT, true);
			updateHarbingerLanding(points, LandingPointsEnum.MONUMENT, false);
			stopLanding(id);
		} else {
			updateRedemptionLanding(points, LandingPointsEnum.MONUMENT, false);
			updateHarbingerLanding(points, LandingPointsEnum.MONUMENT, true);
			stopLanding(id);
		}
		lsl.setType(LandingSpecialStateType.NO_ACTIVE);
		AbyssLandingSpecialService.onSave(lsl);
	}

		/**
	 * 指挥官相关奖励积分入账。
	 * Apply commander reward points for the given race/location.
	 *
	 * 阵营 / race
	 * @param id 地点 ID / location id
	 * points
	 */
	public void onRewardCommander(Race race, int id, int points) {
		if (race == Race.ASMODIANS) {
			updateHarbingerLanding(points, LandingPointsEnum.COMMANDER, true);
			GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(id);
		} else {
			updateRedemptionLanding(points, LandingPointsEnum.COMMANDER, true);
			GameLocationBootstrapServices.abyssLandingSpecialService().startLanding(id);
		}
	}

	/**
	 * 指挥官阵亡时的积分处理。
	 * Apply commander-death point changes for the given race/location.
	 *
	 * 阵营 / race
	 * @param id 地点 ID / location id
	 * points
	 */
	public void onDieCommander(Race race, int id, int points) {
		if (race == Race.ELYOS) {
			updateRedemptionLanding(points, LandingPointsEnum.COMMANDER, true);
			updateHarbingerLanding(points, LandingPointsEnum.COMMANDER, false);
			GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(id);
		} else {
			updateRedemptionLanding(points, LandingPointsEnum.COMMANDER, false);
			updateHarbingerLanding(points, LandingPointsEnum.COMMANDER, true);
			GameLocationBootstrapServices.abyssLandingSpecialService().stopLanding(id);
		}
	}

		/**
	 * 设施相关奖励积分入账。
	 * Apply facility reward points for the given race.
	 *
	 * 阵营 / race
	 * points
	 */
	public void onRewardFacility(Race race, int points) {
		if (race == Race.ASMODIANS) {
			updateHarbingerLanding(points, LandingPointsEnum.FACILITY, true);
			updateRedemptionLanding(points, LandingPointsEnum.FACILITY, false);
		} else {
			updateRedemptionLanding(points, LandingPointsEnum.FACILITY, true);
			updateHarbingerLanding(points, LandingPointsEnum.FACILITY, false);
		}
	}

	/**
	 * 玩家进入世界时同步登陆点状态包。
	 * Sync landing-state packets when a player enters the world.
	 *
	 * @param player 进入世界的玩家 / entering player
	 */
	public void onEnterWorld(Player player) {
		PacketSendUtility.sendPacket(player, new SM_ABYSS_LANDING());
		PacketSendUtility.sendPacket(player,
				new SM_ABYSS_LANDING_LEVEL(0, redemptionLanding().getLevel(), redemptionLanding().getLevel()));
		PacketSendUtility.sendPacket(player,
				new SM_ABYSS_LANDING_LEVEL(1, harbingerLanding().getLevel(), harbingerLanding().getLevel()));
	}

	/**
	 * 持久化/刷新全部登陆点状态。
	 * Persist or refresh all landing locations.
	 */
	public void onUpdate() {
		getDAO().updateLocation(getLandingLocation(redemptionLanding().getId()));
		getDAO().updateLocation(getLandingLocation(harbingerLanding().getId()));
	}

	private AbyssLandingDAO getDAO() {
		return DAOManager.getDAO(AbyssLandingDAO.class);
	}

	/**
	 * 向指定玩家发送登陆点状态包。
	 * Send landing-state packets to the given player.
	 *
	 * target player
	 */
	public void sendPacketToPlayer(Player player) {
		PacketSendUtility.sendPacket(player, new SM_ABYSS_LANDING());
	}

	/**
	 * 获取 AbyssLandingService 单例（Spring 提供者优先，否则 holder）。
	 * Return the AbyssLandingService singleton (Spring provider first, else holder).
	 *
	 * service instance
	 */
	public static AbyssLandingService getInstance() {
		ObjectProvider<AbyssLandingService> provider = instanceProvider;
		if (provider == null) {
			return AbyssLandingService.SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> AbyssLandingService.SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider，供 getInstance 使用。
	 * Inject the Spring ObjectProvider used by getInstance().
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<AbyssLandingService> instanceProvider) {
		AbyssLandingService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final AbyssLandingService instance = new AbyssLandingService();
	}

	/**
	 * 按 ID 获取登陆点。
	 * Get a landing location by id.
	 *
	 * @param id 登陆点 ID / landing location id
	 * landing location
	 */
	public LandingLocation getLandingLocation(int id) {
		return abyssLanding.get(id);
	}

	/**
	 * 返回救赎登陆点。
	 * Return the Redemption landing location.
	 *
	 * @return 救赎登陆点 / Redemption landing
	 */
	public LandingLocation redemptionLanding() {
		return abyssLanding.get(1);
	}

	/**
	 * 返回先驱登陆点。
	 * Return the Harbinger landing location.
	 *
	 * @return 先驱登陆点 / Harbinger landing
	 */
	public LandingLocation harbingerLanding() {
		return abyssLanding.get(2);
	}

	/**
	 * 返回全部登陆点映射。
	 * Return the map of all landing locations.
	 *
	 * location map
	 */
	public static Map<Integer, LandingLocation> getLandingLocations() {
		return abyssLanding;
	}
}
