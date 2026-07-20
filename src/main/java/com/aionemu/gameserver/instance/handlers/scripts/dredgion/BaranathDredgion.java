package com.aionemu.gameserver.instance.handlers.scripts.dredgion;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.DredgionReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.DredgionPlayerReward;
import com.aionemu.gameserver.model.instance.playerreward.InstancePlayerReward;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;

/**
 * 巴拉纳特无渊号副本事件处理器。
 * Instance event handler for Baranath Dredgion.
 *
 * @author Encom
 * @author MATTY
 */

@InstanceID(300110000)
public class BaranathDredgion extends GeneralInstanceHandler
{
	/** 无畏舰奖励 / dredgion reward */
		protected DredgionReward dredgionReward;
	/** 败方倍率 / losing-group multiplier */
		private float loosingGroupMultiplier = 1;
	private static final String STATE = "baranath.";
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * 玩家 / player
	 * result
	 */
	
	protected DredgionPlayerReward getPlayerReward(Player player) {
		Integer object = player.getObjectId();
		if (dredgionReward.getPlayerReward(object) == null) {
			addPlayerToReward(player);
		}
		return (DredgionPlayerReward) dredgionReward.getPlayerReward(object);
	}
	/**
	 * 处理 captureRoom。
	 * Handle captureRoom.
	 *
	 * 阵营 / race
	 * roomId
	 */
	
	protected void captureRoom(Race race, int roomId) {
		dredgionReward.getDredgionRoomById(roomId).captureRoom(race);
	}
	
	private void addPlayerToReward(Player player) {
		DredgionPlayerReward reward = new DredgionPlayerReward(player.getObjectId());
		reward.addPoints(runtimeState().getInt(playerState(player.getObjectId(), "points"), 0));
		for (int i = 0; i < runtimeState().getInt(playerState(player.getObjectId(), "pvp"), 0); i++) {
			reward.addPvPKillToPlayer();
		}
		for (int i = 0; i < runtimeState().getInt(playerState(player.getObjectId(), "monster"), 0); i++) {
			reward.addMonsterKillToPlayer();
		}
		for (int i = 0; i < runtimeState().getInt(playerState(player.getObjectId(), "zones"), 0); i++) {
			reward.captureZone();
		}
		dredgionReward.addPlayerReward(reward);
	}
	
	private boolean containPlayer(Integer object) {
		return dredgionReward.containPlayer(object);
	}
	private void onDieSurkan(Npc npc, Player mostPlayerDamage) {
        Race race = mostPlayerDamage.getRace();
		int roomId = npc.getNpcId() + 14 - 700498;
        captureRoom(race, roomId); //Captain's Cabin Power Surkana.
		runtimeState().put(STATE + "room." + roomId, race.name());
        for (Player player: instance.getPlayersInside()) {
            PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400199, new DescriptionId(race.equals(Race.ASMODIANS) ? 1800483 : 1800481), new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1)));
        }
		int surkanaKills = runtimeState().getInt(STATE + "surkana", 0) + 1;
		runtimeState().put(STATE + "surkana", surkanaKills);
		RetailConditionSpawnEngine.setVariable(instance, "surkana_8", 1, 1);
		if (surkanaKills == 5) {
            // 阿达蒂船长已出现在船长室。 / Captain Adhati has appeared in the Captain's Cabin.
			sendMsgByRace(1400405, Race.PC_ALL, 0);
        }
		getPlayerReward(mostPlayerDamage).captureZone();
		persistPlayerReward(getPlayerReward(mostPlayerDamage));
		npc.getController().onDelete();
    }
	/**
	 * 启动副本计时/任务。
	 * Start instance timer/tasks.
	 */
	
	protected void startInstanceTask() {
		long startedAt = runtimeState().getLong(STATE + "started_at", 0);
		if (startedAt == 0) {
			startedAt = System.currentTimeMillis();
			runtimeState().put(STATE + "started_at", startedAt);
			runtimeState().put(STATE + "phase", "PREPARING");
		}
		scheduleDeadline("start", startedAt + 60_000, this::startProgress);
		scheduleDeadline("teleport", startedAt + 600_000, this::activateCentralTeleporters);
		scheduleDeadline("finish", startedAt + 3_600_000, this::finishByScore);
		long bossFinish = runtimeState().getLong(STATE + "boss_finish_deadline", 0);
		if (bossFinish > 0) scheduleDeadline("boss_finish", bossFinish, this::finishByScore);
	}

	private void startProgress() {
		if (runtimeState().getBoolean(STATE + "settled", false)) {
			return;
		}
		openFirstDoors();
		runtimeState().put(STATE + "phase", "START_PROGRESS");
		dredgionReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendMsgByRace(1400595, Race.PC_ALL, 0);
		sendMsgByRace(1400596, Race.PC_ALL, 0);
		spawnOpeningNamed();
		sendPacket();
	}

	private void spawnOpeningNamed() {
		if (runtimeState().getBoolean(STATE + "opening_spawned", false)) {
			return;
		}
		runtimeState().put(STATE + "opening_spawned", true);
		int side = runtimeState().getInt(STATE + "opening_side", 0);
		if (side == 0) {
			side = Rnd.get(1, 2);
			runtimeState().put(STATE + "opening_side", side);
		}
		spawn(215391, side == 1 ? 415.2769f : 556.53534f, side == 1 ? 282.0216f : 279.2918f,
			409.7311f, side == 1 ? (byte) 118 : (byte) 33);
		int captain = runtimeState().getInt(STATE + "opening_captain", 0);
		if (captain == 0) {
			captain = Rnd.get(1, 2) == 1 ? 215086 : 215390;
			runtimeState().put(STATE + "opening_captain", captain);
		}
		spawn(captain, 485.25455f, 877.04614f, 405.01407f, (byte) 90);
	}

	private void activateCentralTeleporters() {
		if (runtimeState().getBoolean(STATE + "settled", false)
				|| runtimeState().getBoolean(STATE + "teleporters", false)) {
			return;
		}
		runtimeState().put(STATE + "teleporters", true);
		sendMsgByRace(1400265, Race.PC_ALL, 0);
		spawn(730187, 402.33234f, 175.00366f, 433.94046f, (byte) 0, 10);
		spawn(730188, 567.36017f, 175.28262f, 433.92926f, (byte) 0, 9);
	}

	private void finishByScore() {
		if (!runtimeState().getBoolean(STATE + "settled", false)) {
			stopInstance(dredgionReward.getWinningRaceByScore());
		}
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
		int point = retailScore(npc);
		Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
		Race race = mostPlayerDamage.getRace();
		runtimeState().put(STATE + "dead." + npc.getNpcId(), true);
		switch (npc.getObjectTemplate().getTemplateId()) {
		   /**
	 * 解救囚犯：击杀囚室命名怪可获得房间钥匙。 / Rescue Prisoners: olding the named monster of a prisoner receiving chamber can accommodate prisoners get a room key. When you open the container chamber prisoner standing in the room with the key to rescue the prisoners to obtain a score of 100 points. Conversely, it is possible to obtain a 100-point touch the opponent, like captive species
	 */
		    case 798323: //Captured Elyos Scholar.
            case 798324: //Captured Guardian.
            case 798325: //Captured Guardian.
			case 798327: //Captured Asmodian Scholar.
            case 798328: //Captured Archon.
            case 798329: //Captured Archon.
				despawnNpc(npc);
            break;
		   /**
	 * 苏卡纳：摧毁各房间苏卡纳可获得更高分数。 / The Surkana: 1. Destroy Surkana in each room can obtain a higher score. 2. When you add monsters to attack Surkana is around 20m range. First, it is safe to be cleaned up monsters. 3. When you destroy a race that destroyed Surkana is displayed on the map, it is through you can guess the path of the opposing faction
	 */
			case 700485: //Armory Maintenance Surkana.
			case 700486: //Armory Maintenance Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700487: //Gravity Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700488: //Nuclear Control Surkana.
			case 700489: //Nuclear Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700490: //Main Cannon Control Surkana.
			case 700491: //Main Cannon Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700492: //Drop Device Surkana.
			case 700493: //Drop Device Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700494: //Fighter Enhancing Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700495: //Brig Power Surkana.
			case 700496: //Brig Power Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700497: //Bridge Power Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700498: //Captain's Cabin Power Surkana.
				despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700503: //Portside Door Of Captain's Cabin.
				// 左舷船长室门已被摧毁。 / The Port Captain's Cabin Door has been destroyed.
				sendMsgByRace(1400230, Race.PC_ALL, 0);
			break;
			case 700504: //Starboard Door Of Captain's Cabin.
				// 右舷船长室门已被摧毁。 / The Starboard Captain's Cabin Door has been destroyed.
				sendMsgByRace(1400231, Race.PC_ALL, 0);
			break;
		   /**
	 * 船长室传送装置：在兵营击败监督者拉卡内后激活。 / Captain’s Cabin Teleport Device: This teleporter activates when "Supervisor Lakhane" is defeated in the Barracks. Only the race that defeated "Supervisor Lakhane" can use this teleporter
	 */
			case 215427: //Supervisor Lakhane.
				// 中庭尽头生成了可持续 3 分钟的船长室传送装置。 / A Captain's Cabin Teleport Device that lasts for 3 minutes has been generated at the end of the Atrium.
				sendMsgByRace(1400234, Race.PC_ALL, 0);
				runtimeState().put(STATE + "captain_teleporter", true);
				spawn(730197, 484.72f, 761.41998f, 388.66f, (byte) 0, 91); //Captain's Cabin Teleport Device.
            break;
		   /**
	 * 补给室传送器：兵营中传送发生器被摧毁后激活 / Supply Room Teleporter: This teleporter activates after the destruction of the Teleporter Generator in the Barracks
	 */
			case 700505: //Portside Teleporter Generator.
                despawnNpc(npc);
				// 左舷中央传送器已在逃生舱口生成。 / A Portside Central Teleporter has been generated at the Escape Hatch.
				sendMsgByRace(1400228, Race.PC_ALL, 0);
				runtimeState().put(STATE + "supply_port", true);
				spawn(730213, 402.33429f, 175.11707f, 432.2988f, (byte) 0, 64); //No.1 Nuclear Control Room Teleporter.
            break;
			case 700506: //Starboard Teleporter Generator.
                despawnNpc(npc);
				// 右舷中央传送器已在副逃生舱口生成。 / A Starboard Central Teleporter has been generated at the Secondary Escape Hatch.
				sendMsgByRace(1400229, Race.PC_ALL, 0);
				runtimeState().put(STATE + "supply_starboard", true);
				spawn(730214, 567.59119f, 175.19655f, 432.29999f, (byte) 0, 65); //No.2 Nuclear Control Room Teleporter.
            break;
		   /**
	 * 每台护盾发生器需要 3 个理念物品，共 12 个 / Defense Shield Generator: When the Defense Shield Generator on the Weapons Deck or Lower Weapons deck is demolished, a shield appears in Ready Room 1 or 2. This shield blocks access to the center of the Baranath Dredgion. The Ready Room is the shortest route to the center of the Dredgion, and the quickest route to the opposing race’s area. Different tactics can be used in this area to maximize the Group’s accumulation of points. For example, if one Group decides to destroy the opposing Group’s Shield Generator, it will make it difficult for the opposing Group to reach the center of the Dredgion. In some cases, it might wiser for one Group to destroy their own Defense Shield Generator, and delay engagement with the opposing race in order to accumulate more points
	 */
			case 700501: //Portside Defense Shield.
			case 700502: //Starboard Defense Shield.
				despawnNpc(npc);
			break;
			case 700507: //Portside Defense Shield Generator.
				despawnNpc(npc);
				// 左舷防御护盾已在准备室 1 生成。 / The Portside Defense Shield has been generated in Ready Room 1.
				sendMsgByRace(1400226, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "switch_1_destroyed", 1, 0);
	
			break;
			case 700508: //Starboard Defense Shield Generator.
				despawnNpc(npc);
				// 右舷防御护盾已在准备室 2 生成。 / The Starboard Defense Shield has been generated in Ready Room 2.
				sendMsgByRace(1400227, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "switch_2_destroyed", 1, 0);
				
			break;
		   /**
	 * 舱壁：哨兵开战时激活护盾，阻挡入口。 / The Bulkhead: These shields are activated by the Baranath Churl when first encountered at the beginning of the battle. These shields block the entrance from the Armories to Gravity Control, and can be demolished with attacks, but also have a significant amount of health. Groups often opt to move around the shields instead of demolishing them. It’s worth noting that after a certain amount of time has passed, Technician Sarpa spawns in the Gravity Control room, and gives 1,000 points when defeated. There is also a chance that Adjutant Kalanadi, a Hero grade Named Monster, will spawn. Adjutant Kalanadi has a chance to drop Fabled and Heroic accessories
	 */
			case 700598: //Port Bulkhead.
			case 700599: //Starboard Bulkhead.
				int bulkhead = runtimeState().getInt(STATE + "bulkhead", 0) + 1;
				runtimeState().put(STATE + "bulkhead", bulkhead);
				if (bulkhead == 2) {
					int named = Rnd.get(1, 2) == 1 ? 215082 : 215093;
					runtimeState().put(STATE + "bulkhead_named", named);
					if (named == 215082) {
						spawn(named, 456.3946f, 319.65912f, 402.69315f, (byte) 28);
					} else {
						spawn(named, 513.9867f, 319.86224f, 402.68634f, (byte) 4);
					}
				}
				despawnNpc(npc);
			break;
			case 215083: //Navigator Nevikah.
			case 215084: //Assistant Malakun.
			case 215085: //Adjutant Kundhan.
			case 215087: //Sentinel Garkusa.
			case 215088: //Prison Guard Mahnena.
			case 215089: //Air Captain Girana.
			case 215090: //Vice Air Captain Kai.
			case 215091: //Vice Gun Captain Zha.
			case 215092: //Gun Captain Ankrana.
				int secretCache = runtimeState().getInt(STATE + "secret_cache", 0) + 1;
				runtimeState().put(STATE + "secret_cache", secretCache);
				if (secretCache == 5) {
				    // 战舰宝箱已出现在投放区！ / A Dredgion Treasure Chest has appeared in the Drop Zone!
					sendMsgByRace(1401421, Race.PC_ALL, 0);
					runtimeState().put(STATE + "secret_chest", true);
					spawn(701455, 482.82455f, 496.16556f, 397.28323f, (byte) 92); //Dredgion Opportunity Bundle.
				}
            break;
			case 215082: //Technician Sarpa.
			case 215086: //First Mate Aznaya.
			case 215093: //Adjutant Kalanadi.
			case 215390: //Auditor Nirshaka.
			case 215391: //Quartermaster Vujara.
            break;
            case 214823: //Captain Adhati.
				long finishDeadline = System.currentTimeMillis() + 30_000;
				runtimeState().put(STATE + "boss_finish_deadline", finishDeadline);
				scheduleDeadline("boss_finish", finishDeadline, this::finishByScore);
			break;
        }
		updateScore(mostPlayerDamage, npc, point, false);
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	/**
	 * 处理 openFirstDoors。
	 * Handle openFirstDoors.
	 */
	
    protected void openFirstDoors() {
        openDoor(17);
        openDoor(18);
    }
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		if (!containPlayer(player.getObjectId())) {
			addPlayerToReward(player);
		}
		if (runtimeState().getBoolean(STATE + "settled", false)) {
			settlePlayer(player);
		}
		sendPacket();
	}
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		dredgionReward = new DredgionReward(mapId, instanceId);
		dredgionReward.addPointsByRace(Race.ELYOS, runtimeState().getInt(STATE + "score.ELYOS", 0));
		dredgionReward.addPointsByRace(Race.ASMODIANS, runtimeState().getInt(STATE + "score.ASMODIANS", 0));
		refreshLosingMultiplier();
		String winner = runtimeState().get(STATE + "winner");
		if (winner != null) {
			dredgionReward.setWinningRace(Race.valueOf(winner));
		}
		String phase = runtimeState().get(STATE + "phase", "PREPARING");
		dredgionReward.setInstanceScoreType(InstanceScoreType.valueOf(phase));
		for (var entry : runtimeState().snapshot(STATE + "room.").entrySet()) {
			captureRoom(Race.valueOf(entry.getValue()), Integer.parseInt(entry.getKey().substring((STATE + "room.").length())));
		}
		RetailConditionSpawnEngine.initialize(instance);
		startInstanceTask();
		restoreDynamicObjects();
		if (runtimeState().getBoolean(STATE + "settled", false)) {
			scheduleExit();
		}
	}
	/**
	 * 停止副本并结算。
	 * Stop the instance and settle.
	 *
	 * @param race 阵营 / race
	 */
	
	protected void stopInstance(Race race) {
		if (runtimeState().getBoolean(STATE + "settled", false)) {
			return;
		}
		runtimeState().put(STATE + "settled", true);
		runtimeState().put(STATE + "phase", "END_PROGRESS");
		runtimeState().put(STATE + "winner", race.name());
		dredgionReward.setWinningRace(race);
		dredgionReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		doReward();
		sendPacket();
	}
	/**
	 * 结算并发放奖励。
	 * Settle and grant rewards.
	 */
	
	public void doReward() {
		for (Player player : instance.getPlayersInside()) {
			settlePlayer(player);
		}
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		scheduleExit();
	}

	private int getTime() {
		long result = System.currentTimeMillis() - runtimeState().getLong(STATE + "started_at", System.currentTimeMillis());
		if (result < 60000) {
			return (int) (60000 - result);
		} else if (result < 3600000) {
			return (int) (3600000 - (result - 60000));
		}
		return 0;
	}
	
	/**
	 * 处理玩家复活事件。
	 * Handle a player revive event.
	 *
	 * 玩家 / player
	 * result
	 */
	@Override
    public boolean onReviveEvent(Player player) {
		player.getGameStats().updateStatsAndSpeedVisually();
		PlayerReviveService.revive(player, 100, 100, false, 0);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PacketSendUtility.sendPacket(player, new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_INSTANT_DUNGEON_RESURRECT, 0, 0));
        dredgionReward.portToPosition(player);
		return true;
    }
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * 玩家 / player
	 * @param lastAttacker 最后攻击者 / last attacker
	 * result
	 */
	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		int points = 60;
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId()), true);
        PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), false, 0, 8));
		if (lastAttacker instanceof Player) {
			if (lastAttacker.getRace() != player.getRace()) {
				InstancePlayerReward playerReward = getPlayerReward(player);
				if (getPointsByRace(lastAttacker.getRace()).compareTo(getPointsByRace(player.getRace())) < 0) {
					points *= loosingGroupMultiplier;
				} else if (loosingGroupMultiplier == 10 || playerReward.getPoints() == 0) {
					points = 0;
				}
			    updateScore((Player) lastAttacker, player, points, true);
			}
		}
		updateScore(player, player, -points, false);
		return true;
	}
	
	private MutableInt getPointsByRace(Race race) {
		return dredgionReward.getPointsByRace(race);
	}
	
	private void addPointsByRace(Race race, int points) {
		dredgionReward.addPointsByRace(race, points);
		runtimeState().put(STATE + "score." + race.name(), dredgionReward.getPointsByRace(race).intValue());
	}

	private void addPointToPlayer(Player player, int points) {
		DredgionPlayerReward reward = getPlayerReward(player);
		reward.addPoints(points);
		persistPlayerReward(reward);
	}

	private void addPvPKillToPlayer(Player player) {
		DredgionPlayerReward reward = getPlayerReward(player);
		reward.addPvPKillToPlayer();
		persistPlayerReward(reward);
	}

	private void addBalaurKillToPlayer(Player player) {
		DredgionPlayerReward reward = getPlayerReward(player);
		reward.addMonsterKillToPlayer();
		persistPlayerReward(reward);
	}
	/**
	 * 处理 updateScore。
	 * Handle updateScore.
	 *
	 * 玩家 / player
	 * target
	 * points
	 * pvpKill
	 */
	
	protected void updateScore(Player player, Creature target, int points, boolean pvpKill) {
		if (points == 0) {
			return;
		}
		addPointsByRace(player.getRace(), points);
		List<Player> playersToGainScore = new ArrayList<Player>();
		if (target != null && player.isInGroup2()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				if (member.getLifeStats().isAlreadyDead()) {
					continue;
				} if (MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
					playersToGainScore.add(member);
				}
			}
		} else {
			playersToGainScore.add(player);
		}
		for (Player playerToGainScore : playersToGainScore) {
			addPointToPlayer(playerToGainScore, points / playersToGainScore.size());
			if (target instanceof Npc) {
				PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(((Npc) target).getObjectTemplate().getNameId() * 2 + 1), points));
			} else if (target instanceof Player) {
				PacketSendUtility.sendPacket(playerToGainScore, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
			}
		}
		int pointDifference = getPointsByRace(Race.ASMODIANS).intValue() - (getPointsByRace(Race.ELYOS)).intValue();
		if (pointDifference < 0) {
			pointDifference *= -1;
		} if (pointDifference >= 3000) {
			loosingGroupMultiplier = 10;
		} else if (pointDifference >= 1000) {
			loosingGroupMultiplier = 1.5f;
		} else {
			loosingGroupMultiplier = 1;
		} if (pvpKill && points > 0) {
			addPvPKillToPlayer(player);
		} else if (target instanceof Npc && ((Npc) target).getRace().equals(Race.DRAKAN)) {
			addBalaurKillToPlayer(player);
		}
		sendPacket();
	}

	private void refreshLosingMultiplier() {
		int difference = Math.abs(getPointsByRace(Race.ASMODIANS).intValue()
			- getPointsByRace(Race.ELYOS).intValue());
		loosingGroupMultiplier = difference >= 3000 ? 10 : difference >= 1000 ? 1.5f : 1;
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		dredgionReward.clear();
	}
	/**
	 * 打开指定门。
	 * Open the given door.
	 *
	 * doorId
	 */
	
	protected void openDoor(int doorId) {
		setDoorState(doorId, true);
	}
	
	private void sendPacket() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), dredgionReward, instance.getPlayersInside()));
			}
		});
	}

	private int retailScore(Npc npc) {
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		return score == null ? 0 : score.value();
	}

	private String playerState(int playerId, String field) {
		return STATE + "player." + playerId + "." + field;
	}

	private void persistPlayerReward(InstancePlayerReward reward) {
		int playerId = reward.getOwner();
		runtimeState().put(playerState(playerId, "points"), reward.getPoints());
		runtimeState().put(playerState(playerId, "pvp"), reward.getPvPKills());
		runtimeState().put(playerState(playerId, "monster"), reward.getMonsterKills());
		runtimeState().put(playerState(playerId, "zones"), ((DredgionPlayerReward) reward).getZoneCaptured());
	}

	private void settlePlayer(Player player) {
		InstancePlayerReward playerReward = getPlayerReward(player);
		float abyssPoint = playerReward.getPoints() * RateConfig.DREDGION_REWARD_RATE;
		abyssPoint += player.getRace().equals(dredgionReward.getWinningRace())
			? dredgionReward.getWinnerPoints() : dredgionReward.getLooserPoints();
		RewardPlan plan = new RewardPlan(List.of(), 0, 0, Math.max(0, (int) abyssPoint), 0);
		if (InstanceSettlementService.settle(instance.getDynamicInstance().getInstanceUid(), player, "dredgion", plan)) {
			GameEngineServices.questEngine().onDredgionReward(new QuestEnv(null, player, 0, 0));
		}
	}

	private void scheduleExit() {
		long deadline = runtimeState().getLong(STATE + "exit_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + 120_000;
			runtimeState().put(STATE + "exit_deadline", deadline);
		}
		scheduleDeadline("exit", deadline, () -> {
			for (Player player : instance.getPlayersInside()) {
				if (PlayerActions.isAlreadyDead(player)) {
					PlayerReviveService.duelRevive(player);
				}
				onExitInstance(player);
			}
			GameCoreGameplayServices.autoGroupService().unRegisterInstance(instance);
		});
	}

	private void restoreDynamicObjects() {
		if (runtimeState().getBoolean(STATE + "settled", false)) return;
		if (runtimeState().getBoolean(STATE + "teleporters", false)) {
			spawn(730187, 402.33234f, 175.00366f, 433.94046f, (byte) 0, 10);
			spawn(730188, 567.36017f, 175.28262f, 433.92926f, (byte) 0, 9);
		}
		if (runtimeState().getBoolean(STATE + "captain_teleporter", false)
				&& !runtimeState().getBoolean(STATE + "dead.730197", false)) {
			spawn(730197, 484.72f, 761.41998f, 388.66f, (byte) 0, 91);
		}
		if (runtimeState().getBoolean(STATE + "opening_spawned", false)) {
			int side = runtimeState().getInt(STATE + "opening_side", 1);
			if (!runtimeState().getBoolean(STATE + "dead.215391", false)) {
				spawn(215391, side == 1 ? 415.2769f : 556.53534f, side == 1 ? 282.0216f : 279.2918f,
					409.7311f, side == 1 ? (byte) 118 : (byte) 33);
			}
			int captain = runtimeState().getInt(STATE + "opening_captain", 215086);
			if (!runtimeState().getBoolean(STATE + "dead." + captain, false)) {
				spawn(captain, 485.25455f, 877.04614f, 405.01407f, (byte) 90);
			}
		}
		if (runtimeState().getBoolean(STATE + "supply_port", false)
				&& !runtimeState().getBoolean(STATE + "dead.730213", false)) {
			spawn(730213, 402.33429f, 175.11707f, 432.2988f, (byte) 0, 64);
		}
		if (runtimeState().getBoolean(STATE + "supply_starboard", false)
				&& !runtimeState().getBoolean(STATE + "dead.730214", false)) {
			spawn(730214, 567.59119f, 175.19655f, 432.29999f, (byte) 0, 65);
		}
		if (runtimeState().getBoolean(STATE + "secret_chest", false)
				&& !runtimeState().getBoolean(STATE + "dead.701455", false)) {
			spawn(701455, 482.82455f, 496.16556f, 397.28323f, (byte) 92);
		}
		int named = runtimeState().getInt(STATE + "bulkhead_named", 0);
		if (named > 0 && !runtimeState().getBoolean(STATE + "dead." + named, false)) {
			if (named == 215082) {
				spawn(named, 456.3946f, 319.65912f, 402.69315f, (byte) 28);
			} else if (named == 215093) {
				spawn(named, 513.9867f, 319.86224f, 402.68634f, (byte) 4);
			}
		}
	}
	/**
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * message
	 * 阵营 / race
	 * time
	 */
	
    protected void sendMsgByRace(final int msg, final Race race, int time) {
		instance.doOnAllPlayers((Visitor<Player>) player -> {
			if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
			}
		});
    }
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
	
	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * result
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return dredgionReward;
	}
	
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onExitInstance(Player player) {
        TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
    }
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
    public void onLeaveInstance(Player player) {
		//“玩家名”已离开战斗。 / "Player Name" has left the battle.
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
        if (player.isInGroup2()) {
            PlayerGroupService.removePlayer(player);
        }
    }
}
