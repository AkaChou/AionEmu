package com.aionemu.gameserver.instance.handlers.scripts.dredgion;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

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
		DredgionPlayerReward reward = restorePlayerReward(player.getObjectId());
		dredgionReward.addPlayerReward(reward);
		runtimeState().put(playerState(player.getObjectId(), "race"), player.getRace().name());
		persistPlayerReward(reward);
	}

	private DredgionPlayerReward restorePlayerReward(int playerId) {
		DredgionPlayerReward reward = new DredgionPlayerReward(playerId);
		reward.addPoints(runtimeState().getInt(playerState(playerId, "points"), 0));
		for (int i = 0; i < runtimeState().getInt(playerState(playerId, "pvp"), 0); i++) reward.addPvPKillToPlayer();
		for (int i = 0; i < runtimeState().getInt(playerState(playerId, "monster"), 0); i++) reward.addMonsterKillToPlayer();
		for (int i = 0; i < runtimeState().getInt(playerState(playerId, "zones"), 0); i++) reward.captureZone();
		return reward;
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
		scheduleDeadline("teleport", startedAt + 1_020_000, this::activateTimedTeleporters);
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
		sendPacket();
	}

	private void activateTimedTeleporters() {
		if (!runtimeState().getBoolean(STATE + "settled", false)) {
			sendMsgByRace(1400265, Race.PC_ALL, 0);
			RetailConditionSpawnEngine.setVariable(instance,
				"idab1_dreadgion_teleport_17minuteslater", 1, 0);
		}
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
		int npcId = npc.getNpcId();
		switch (npcId) {
			case 215085 -> {
				sendMsgByRace(1400234, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "teleport_3_destroyed", 1, 0);
			}
			case 700505 -> {
				sendMsgByRace(1400228, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "teleport_1_destroyed", 1, 0);
			}
			case 700506 -> {
				sendMsgByRace(1400229, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "teleport_2_destroyed", 1, 0);
			}
			case 700507 -> {
				sendMsgByRace(1400226, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "switch_1_destroyed", 1, 0);
			}
			case 700508 -> {
				sendMsgByRace(1400227, Race.PC_ALL, 0);
				RetailConditionSpawnEngine.setVariable(instance, "switch_2_destroyed", 1, 0);
			}
		}
		int point = retailScore(npc);
		Player mostPlayerDamage = npc.getAggroList().getMostPlayerDamage();
        if (mostPlayerDamage == null) {
            return;
        }
		switch (npcId) {
		   /**
	 * 解救囚犯：击杀囚室命名怪可获得房间钥匙。 / Rescue Prisoners: olding the named monster of a prisoner receiving chamber can accommodate prisoners get a room key. When you open the container chamber prisoner standing in the room with the key to rescue the prisoners to obtain a score of 100 points. Conversely, it is possible to obtain a 100-point touch the opponent, like captive species
	 */
		    case 798323: //Captured Elyos Scholar.
            case 798324: //Captured Guardian.
            case 798325: //Captured Guardian.
			case 798326: //Captured Guardian.
			case 798327: //Captured Asmodian Scholar.
            case 798328: //Captured Archon.
            case 798329: //Captured Archon.
			case 798330: //Captured Archon.
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
			case 700501: //Portside Defense Shield.
			case 700502: //Starboard Defense Shield.
			case 700505: //Portside Teleporter Generator.
			case 700506: //Starboard Teleporter Generator.
			case 700507: //Portside Defense Shield Generator.
			case 700508: //Starboard Defense Shield Generator.
			case 700598: //Port Bulkhead.
			case 700599: //Starboard Bulkhead.
				despawnNpc(npc);
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
		runtimeState().put(playerState(player.getObjectId(), "race"), player.getRace().name());
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
		restorePlayers();
		for (var entry : runtimeState().snapshot(STATE + "room.").entrySet()) {
			captureRoom(Race.valueOf(entry.getValue()), Integer.parseInt(entry.getKey().substring((STATE + "room.").length())));
		}
		if (runtimeState().getBoolean(STATE + "settled", false)) {
			doReward();
		} else {
			RetailConditionSpawnEngine.initialize(instance);
			startInstanceTask();
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
		RetailConditionSpawnEngine.clear(instance);
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
		RetailConditionSpawnEngine.clear(instance);
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

	private void restorePlayers() {
		Set<Integer> players = new HashSet<>();
		for (String key : runtimeState().snapshot(STATE + "player.").keySet()) {
			String suffix = key.substring((STATE + "player.").length());
			int separator = suffix.indexOf('.');
			if (separator > 0) {
				players.add(Integer.parseInt(suffix.substring(0, separator)));
			}
		}
		for (int playerId : players) {
			dredgionReward.addPlayerReward(restorePlayerReward(playerId));
		}
	}

	private Race playerRace(int playerId) {
		String race = runtimeState().get(playerState(playerId, "race"));
		return race == null ? null : Race.valueOf(race);
	}

	private RewardPlan rewardPlan(InstancePlayerReward playerReward, Race race) {
		return InstanceSettlementService.dredgionPlan(playerReward.getPoints(), RateConfig.DREDGION_REWARD_RATE,
				race == dredgionReward.getWinningRace(), dredgionReward.getWinnerPoints(), dredgionReward.getLooserPoints());
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
