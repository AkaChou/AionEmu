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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 钱特拉无渊号副本事件处理器。
 * Instance event handler for Chantra Dredgion.
 *
 * @author Encom
 * @author MATTY
 */

@InstanceID(300210000)
public class ChantraDredgionInstance extends GeneralInstanceHandler
{
	/** 无畏舰奖励 / dredgion reward */
		protected DredgionReward dredgionReward;
	/** 败方倍率 / losing-group multiplier */
		private float loosingGroupMultiplier = 1;
	private static final String STATE = "chantra.";
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
		DredgionPlayerReward reward = restorePlayerReward(player.getObjectId());
		dredgionReward.addPlayerReward(reward);
		runtimeState().put(playerState(player.getObjectId(), "race"), player.getRace().name());
		persistPlayerReward(reward);
	}
	
	private boolean containPlayer(Integer object) {
		return dredgionReward.containPlayer(object);
	}
	private DredgionPlayerReward restorePlayerReward(int playerId) {
		DredgionPlayerReward reward = new DredgionPlayerReward(playerId);
		reward.addPoints(runtimeState().getInt(playerState(playerId, "points"), 0));
		for (int i = 0; i < runtimeState().getInt(playerState(playerId, "pvp"), 0); i++) reward.addPvPKillToPlayer();
		for (int i = 0; i < runtimeState().getInt(playerState(playerId, "monster"), 0); i++) reward.addMonsterKillToPlayer();
		for (int i = 0; i < runtimeState().getInt(playerState(playerId, "zones"), 0); i++) reward.captureZone();
		return reward;
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

	private void restorePlayers() {
		Set<Integer> players = new HashSet<>();
		for (String key : runtimeState().snapshot(STATE + "player.").keySet()) {
			String suffix = key.substring((STATE + "player.").length());
			int separator = suffix.indexOf('.');
			if (separator > 0) players.add(Integer.parseInt(suffix.substring(0, separator)));
		}
		for (int playerId : players) dredgionReward.addPlayerReward(restorePlayerReward(playerId));
	}

	private Race playerRace(int playerId) {
		String race = runtimeState().get(playerState(playerId, "race"));
		return race == null ? null : Race.valueOf(race);
	}

	private RewardPlan rewardPlan(InstancePlayerReward playerReward, Race race) {
		return InstanceSettlementService.dredgionPlan(playerReward.getPoints(), RateConfig.DREDGION_REWARD_RATE,
				race == dredgionReward.getWinningRace(), dredgionReward.getWinnerPoints(), dredgionReward.getLooserPoints());
	}

	private void onDieSurkan(Npc npc, Player mostPlayerDamage) {
		Race race = mostPlayerDamage.getRace();
		captureRoom(race, npc.getNpcId() + 14 - 700851); //Captain's Cabin Power Surkana.
		runtimeState().put(STATE + "room." + (npc.getNpcId() + 14 - 700851), race.name());
		for (Player player: instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400199, new DescriptionId(race.equals(Race.ASMODIANS) ? 1800483 : 1800481), new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1)));
		}
		int surkanaKills = runtimeState().getInt(STATE + "surkana", 0) + 1;
		runtimeState().put(STATE + "surkana", surkanaKills);
		if (surkanaKills == 5) {
            // 扎纳塔船长已出现在船长室。 / Captain Zanata has appeared in the Captain's Cabin.
			sendMsgByRace(1400632, Race.PC_ALL, 0);
			runtimeState().put(STATE + "captain_spawned", true);
			spawn(216886, 485.47916f, 812.4957f, 416.68475f, (byte) 31);
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
		long namedDeadline = runtimeState().getLong(STATE + "named_deadline", 0);
		if (namedDeadline == 0) {
			namedDeadline = startedAt + Rnd.get(750, 900) * 1000L;
			runtimeState().put(STATE + "named_deadline", namedDeadline);
		}
		scheduleDeadline("named", namedDeadline, this::spawnTimedNamed);
		scheduleDeadline("finish", startedAt + 3_600_000, this::finishByScore);
		long bossFinish = runtimeState().getLong(STATE + "boss_finish_deadline", 0);
		if (bossFinish > 0) scheduleDeadline("boss_finish", bossFinish, this::finishByScore);
	}

	private void startProgress() {
		if (runtimeState().getBoolean(STATE + "settled", false)) return;
		openFirstDoors();
		runtimeState().put(STATE + "phase", "START_PROGRESS");
		dredgionReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendMsgByRace(1400604, Race.PC_ALL, 0);
		sendMsgByRace(1400605, Race.PC_ALL, 0);
		if (!runtimeState().getBoolean(STATE + "opening_spawned", false)) {
			runtimeState().put(STATE + "opening_spawned", true);
			int side = Rnd.get(1, 2);
			int captain = Rnd.get(1, 2) == 1 ? 216887 : 216885;
			runtimeState().put(STATE + "opening_side", side);
			runtimeState().put(STATE + "opening_captain", captain);
			spawn(216888, side == 1 ? 415.2769f : 556.53534f, side == 1 ? 282.0216f : 279.2918f,
				409.7311f, side == 1 ? (byte) 118 : (byte) 33);
			spawn(captain, 485.25455f, 877.04614f, 405.01407f, (byte) 90);
		}
		sendPacket();
	}

	private void activateCentralTeleporters() {
		if (runtimeState().getBoolean(STATE + "settled", false)
				|| runtimeState().getBoolean(STATE + "teleporters", false)) return;
		runtimeState().put(STATE + "teleporters", true);
		sendMsgByRace(1401424, Race.PC_ALL, 0);
		spawn(730311, 415.033875f, 174.003876f, 433.94046f, (byte) 0, 34);
		spawn(730312, 572.038208f, 185.252136f, 433.94046f, (byte) 0, 10);
	}

	private void spawnTimedNamed() {
		if (runtimeState().getBoolean(STATE + "settled", false)
				|| runtimeState().getBoolean(STATE + "timed_named", false)) return;
		runtimeState().put(STATE + "timed_named", true);
		sendMsgByRace(1400633, Race.PC_ALL, 0);
		spawn(216941, 479.955719f, 314.959381f, 412.0f, (byte) 30);
	}

	private void finishByScore() {
		if (!runtimeState().getBoolean(STATE + "settled", false)) stopInstance(dredgionReward.getWinningRaceByScore());
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
	 * 钱特拉战舰入口附近有 6 个武器箱，摧毁各得 100 分。 / There are six weapon chests located near the Chantra Dredgion entrance, and each chest awards 100 points if destroyed. These chests are also related to Quests for both Elyos and Asmodians
	 */
		    case 700836: //Weapon Chest.
				despawnNpc(npc);
            break;
		   /**
	 * 苏卡纳：摧毁各房间苏卡纳可获得更高分数。 / The Surkana: Destroy Surkana in each room can obtain a higher score. 2. When you add monsters to attack Surkana is around 20m range. First, it is safe to be cleaned up monsters. 3. When you destroy a race that destroyed Surkana is displayed on the map. It is through you can guess the path of the opposing faction. 4. Captain Room Teleport appeared to be destroyed 5 Surkana
	 */
			case 700838: //Armory Maintenance Surkana.
			case 700839: //Armory Maintenance Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700840: //Gravity Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700841: //Nuclear Control Surkana.
			case 700842: //Nuclear Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700843: //Main Cannon Control Surkana.
			case 700844: //Main Cannon Control Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700845: //Drop Device Surkana.
			case 700846: //Drop Device Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700847: //Fighter Enhancing Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700848: //Storage Power Surkana.
			case 700849: //Storage Power Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700850: //Bridge Power Surkana.
			    despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
			case 700851: //Captain's Cabin Power Surkana.
				despawnNpc(npc);
				onDieSurkan(npc, mostPlayerDamage);
			break;
		   /**
	 * 船长室通道：二楼船长室左右有路但门被封，需特殊方式开启。 / Captain’s Cabin Passage: There are paths to the left and right of the Captain’s Cabin’s on the second floor, but the doors are blocked. These doors cannot be demolished, and can only be opened with a key dropped by a specific Named Monster. Groups desiring the Captain’s Cabin Passage Key will need to defeat "Sahadena The Abettor" in the center of the Dredgion. Only one Group can loot the key. The Captain’s Cabin Teleport Device is located just beyond the Barracks, and can make reaching Captain Zanata much easier
	 */
			case 216882: //Sahadena The Abettor.
				if (race.equals(Race.ELYOS)) {
				   // 船长室传送装置已在中庭尽头生成。 / Captain's Cabin teleport device has been created at the end of the Atrium.
				   sendMsgByRace(1400652, Race.ELYOS, 0);
				   RetailConditionSpawnEngine.setVariable(instance, "teleport_l_destroyed", 1, 0);
				} else if (race.equals(Race.ASMODIANS)) {
				   // 船长室传送装置已在中庭尽头生成。 / Captain's Cabin teleport device has been created at the end of the Atrium.
				   sendMsgByRace(1400652, Race.ASMODIANS, 0);
				   RetailConditionSpawnEngine.setVariable(instance, "teleport_d_destroyed", 1, 0);
				}
            break;
		   /**
	 * 补给室传送器：兵营中传送发生器被摧毁后激活 / Supply Room Teleporter: This teleporter activates after the destruction of the Teleporter Generator in the Barracks
	 */
			case 730349: //Portside Teleporter Generator.
                despawnNpc(npc);
				// 物资仓库传送装置已在副逃生舱口生成。 / Supplies Storage teleport device has been created at Escape Hatch.
				sendMsgByRace(1400631, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "teleport_4_destroyed", 1, 0);
            break;
			case 730350: //Starboard Teleporter Generator.
                despawnNpc(npc);
				// 物资仓库传送装置已在副逃生舱口生成。 / Supplies Storage teleport device has been created at the Secondary Escape Hatch.
				sendMsgByRace(1400641, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "teleport_5_destroyed", 1, 0);
            break;
		   /**
	 * 每台护盾发生器需要 3 个理念物品，共 12 个 / Defense Shield Generator: When the Defense Shield Generator on the Weapons Deck or Lower Weapons deck is demolished, a shield appears in Ready Room 1 or 2. This shield blocks access to the center of the Chantra Dredgion. The Ready Room is the shortest route to the center of the Dredgion, and the quickest route to the opposing race’s area. Different tactics can be used in this area to maximize the Group’s accumulation of points. For example, if one Group decides to destroy the opposing Group’s Shield Generator, it will make it difficult for the opposing Group to reach the center of the Dredgion. In some cases, it might wiser for one Group to destroy their own Defense Shield Generator, and delay engagement with the opposing race in order to accumulate more points
	 */
			case 730345: //Portside Defense Shield.
			case 730346: //Starboard Defense Shield.
				despawnNpc(npc);
			break;
			case 730351: //Portside Defense Shield Generator.
				despawnNpc(npc);
				// 左舷防御护盾已在准备室 1 生成。 / The Portside Defense Shield has been generated in Ready Room 1.
				sendMsgByRace(1400226, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "switch_1_destroyed", 1, 0);
			break;
			case 730352: //Starboard Defense Shield Generator.
				despawnNpc(npc);
				// 右舷防御护盾已在准备室 2 生成。 / The Starboard Defense Shield has been generated in Ready Room 2.
				sendMsgByRace(1400227, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "switch_2_destroyed", 1, 0);
			break;
		   /**
	 * 舱壁：钱特拉哨兵开战时激活护盾，阻挡入口。 / The Bulkhead: These shields are activated by the Chantra Sentinel when first encountered at the beginning of the battle. These shields block the entrance from the Armories to Gravity Control, and can be demolished with attacks, but also have a significant amount of health. Groups often opt to move around the shields instead of demolishing them. It’s worth noting that after a certain amount of time has passed, Officer Kamanya spawns in the Gravity Control room, and gives 1,000 points when defeated. There is also a chance that Rajaya the Inquisitor, a Hero grade Named Monster, will spawn. Rajaya the Inquisitor has a chance to drop Fabled and Heroic accessories
	 */
			case 730353: //Port Bulkhead.
			case 730354: //Starboard Bulkhead.
				int bulkhead = runtimeState().getInt(STATE + "bulkhead", 0) + 1;
				runtimeState().put(STATE + "bulkhead", bulkhead);
				if (bulkhead == 2) {
					int named = Rnd.get(1, 2) == 1 ? 216889 : 216875;
					runtimeState().put(STATE + "bulkhead_named", named);
					if (named == 216889) {
						spawn(named, 456.3946f, 319.65912f, 402.69315f, (byte) 28);
					} else {
						spawn(named, 513.9867f, 319.86224f, 402.68634f, (byte) 4);
					}
				}
				despawnNpc(npc);
			break;
			case 216875: //Shipmate Badala.
			case 216876: //Horizonist Anuta.
			case 216877: //First Mate Rukana.
			case 216878: //Skylord Vundar.
			case 216879: //First Mate Dubakar.
			case 216880: //Chief Daraka.
			case 216881: //Trigger.
			case 216883: //Quartermaster Nupakun.
			case 216884: //Takahan.
			case 217037: //Gatekeeper Sarta.
				int secretCache = runtimeState().getInt(STATE + "secret_cache", 0) + 1;
				runtimeState().put(STATE + "secret_cache", secretCache);
				if (secretCache == 6) {
				    // 战舰宝箱已出现在投放区！ / A Dredgion Treasure Chest has appeared in the Drop Zone!
					sendMsgByRace(1401421, Race.PC_ALL, 0);
					runtimeState().put(STATE + "secret_chest", true);
					spawn(701455, 482.82455f, 496.16556f, 397.28323f, (byte) 92); //Dredgion Opportunity Bundle.
				}
            break;
			case 216885: //Hookmatan.
            break;
			case 216887: //Skyguard Parishka.
			case 216889: //Rajaya The Inquisitor.
			case 216888: //Quartermaster Bhati.
			case 216890: //Windfinder Kumar.
			case 216941: //Officier Kamanya.
			break;
			case 216886: //Captain Zanata.
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
		openDoor(4);
		openDoor(173);
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
		runtimeState().put(playerState(player.getObjectId(), "race"), player.getRace().name());
		if (runtimeState().getBoolean(STATE + "settled", false)) settlePlayer(player);
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
		if (winner != null) dredgionReward.setWinningRace(Race.valueOf(winner));
		dredgionReward.setInstanceScoreType(InstanceScoreType.valueOf(runtimeState().get(STATE + "phase", "PREPARING")));
		restorePlayers();
		for (var entry : runtimeState().snapshot(STATE + "room.").entrySet()) {
			captureRoom(Race.valueOf(entry.getValue()), Integer.parseInt(entry.getKey().substring((STATE + "room.").length())));
		}
		RetailConditionSpawnEngine.initialize(instance);
		startInstanceTask();
		restoreDynamicObjects();
		if (runtimeState().getBoolean(STATE + "settled", false)) doReward();
	}
	/**
	 * 停止副本并结算。
	 * Stop the instance and settle.
	 *
	 * @param race 阵营 / race
	 */
	
	protected void stopInstance(Race race) {
		if (runtimeState().getBoolean(STATE + "settled", false)) return;
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
		for (DredgionPlayerReward playerReward : List.copyOf(dredgionReward.getInstanceRewards())) {
			Player player = instance.getPlayer(playerReward.getOwner());
			if (player != null) {
				settlePlayer(player);
			} else {
				Race race = playerRace(playerReward.getOwner());
				if (race != null) {
					InstanceSettlementService.queue(instance, playerReward.getOwner(), "dredgion",
							rewardPlan(playerReward, race));
				}
			}
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

	private void refreshLosingMultiplier() {
		int difference = Math.abs(getPointsByRace(Race.ASMODIANS).intValue()
			- getPointsByRace(Race.ELYOS).intValue());
		loosingGroupMultiplier = difference >= 3000 ? 10 : difference >= 1000 ? 1.5f : 1;
	}

	private void settlePlayer(Player player) {
		InstancePlayerReward playerReward = getPlayerReward(player);
		runtimeState().put(playerState(player.getObjectId(), "race"), player.getRace().name());
		RewardPlan plan = rewardPlan(playerReward, player.getRace());
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
			spawn(730311, 415.033875f, 174.003876f, 433.94046f, (byte) 0, 34);
			spawn(730312, 572.038208f, 185.252136f, 433.94046f, (byte) 0, 10);
		}
		if (runtimeState().getBoolean(STATE + "opening_spawned", false)) {
			int side = runtimeState().getInt(STATE + "opening_side", 1);
			if (!runtimeState().getBoolean(STATE + "dead.216888", false)) {
				spawn(216888, side == 1 ? 415.2769f : 556.53534f, side == 1 ? 282.0216f : 279.2918f,
					409.7311f, side == 1 ? (byte) 118 : (byte) 33);
			}
			int captain = runtimeState().getInt(STATE + "opening_captain", 216885);
			if (!runtimeState().getBoolean(STATE + "dead." + captain, false)) {
				spawn(captain, 485.25455f, 877.04614f, 405.01407f, (byte) 90);
			}
		}
		if (runtimeState().getBoolean(STATE + "timed_named", false)
				&& !runtimeState().getBoolean(STATE + "dead.216941", false)) {
			spawn(216941, 479.955719f, 314.959381f, 412.0f, (byte) 30);
		}
		if (runtimeState().getBoolean(STATE + "captain_spawned", false)
				&& !runtimeState().getBoolean(STATE + "dead.216886", false)) {
			spawn(216886, 485.47916f, 812.4957f, 416.68475f, (byte) 31);
		}
		if (runtimeState().getBoolean(STATE + "secret_chest", false)
				&& !runtimeState().getBoolean(STATE + "dead.701455", false)) {
			spawn(701455, 482.82455f, 496.16556f, 397.28323f, (byte) 92);
		}
		int named = runtimeState().getInt(STATE + "bulkhead_named", 0);
		if (named > 0 && !runtimeState().getBoolean(STATE + "dead." + named, false)) {
			if (named == 216889) {
				spawn(named, 456.3946f, 319.65912f, 402.69315f, (byte) 28);
			} else if (named == 216875) {
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
