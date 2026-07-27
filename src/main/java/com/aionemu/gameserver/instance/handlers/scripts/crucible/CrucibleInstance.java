package com.aionemu.gameserver.instance.handlers.scripts.crucible;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.StageType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.CruciblePlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_STAGE_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

public class CrucibleInstance extends GeneralInstanceHandler {

	protected InstanceReward<CruciblePlayerReward> instanceReward;
	private StageType stageType = StageType.DEFAULT;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new InstanceReward<>(mapId, instanceId);
		String stage = runtimeState().get("crucible.stage");
		if (stage != null) {
			stageType = StageType.valueOf(stage);
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		if (!instanceReward.containPlayer(player.getObjectId())) {
			CruciblePlayerReward reward = new CruciblePlayerReward(player.getObjectId());
			reward.addPoints(runtimeState().getInt(playerKey(player.getObjectId(), "points"), 0));
			reward.setInsignia(runtimeState().getInt(playerKey(player.getObjectId(), "insignia"), 0));
			reward.setParticipationState(runtimeState().getInt(playerKey(player.getObjectId(), "participation"),
					CruciblePlayerReward.PARTICIPATION_NONE));
			if (runtimeState().getBoolean(playerKey(player.getObjectId(), "rewarded"), false)) {
				reward.setRewarded();
			}
			instanceReward.addPlayerReward(reward);
		}
		CruciblePlayerReward reward = getPlayerReward(player.getObjectId());
		if (reward.getParticipationState() == CruciblePlayerReward.PARTICIPATION_NONE) {
			setParticipationState(reward, stageType == StageType.DEFAULT
					? CruciblePlayerReward.PARTICIPATION_PLAYING : CruciblePlayerReward.PARTICIPATION_WAITING);
			if (reward.getParticipationState() == CruciblePlayerReward.PARTICIPATION_WAITING) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_ENTERED_BIRTHAREA_IDARENA);
			}
		}
		sendScore(player);
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	@Override
	public StageType getStage() {
		return stageType;
	}

	@Override
	public void onChangeStage(StageType type) {
		stageType = type;
		runtimeState().put("crucible.stage", type.name());
		for (Player player : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_INSTANCE_STAGE_INFO(2, type.getId(), type.getType()));
		}
	}

	@Override
	public boolean supportsRetailNpcScore(int npcId, int scoreApplyType) {
		if (DataManager.RETAIL_AI_DATA == null || scoreApplyType != 0) {
			return false;
		}
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npcId);
		return score != null && score.scoreApplyType() == 0 && score.equalizingScore() == 0;
	}

	@Override
	public boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int points) {
		return supportsRetailNpcScore(npc.getNpcId(), scoreApplyType) && consumeNpcScore(npc, points);
	}

	@Override
	public void onDie(Npc npc) {
		RetailConditionSpawnEngine.consumeConditionSpawnDeath(instance, npc);
		if (DataManager.RETAIL_AI_DATA == null) {
			return;
		}
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score != null && supportsRetailNpcScore(npc.getNpcId(), score.scoreApplyType())) {
			consumeNpcScore(npc, score.value());
		}
	}

	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		CruciblePlayerReward reward = getPlayerReward(player.getObjectId());
		if (reward != null) {
			setParticipationState(reward, CruciblePlayerReward.PARTICIPATION_WAITING);
		}
		return false;
	}

	@Override
	public boolean onReviveEvent(Player player) {
		player.getGameStats().updateStatsAndSpeedVisually();
		PlayerReviveService.revive(player, 100, 100, false, 0);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PacketSendUtility.sendPacket(player,
				new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_IDARENA_RESURRECT, 0, 0));
		return true;
	}

	@Override
	public void onInstanceDestroy() {
		instanceReward.clear();
	}

	protected CruciblePlayerReward getPlayerReward(int playerId) {
		return (CruciblePlayerReward) instanceReward.getPlayerReward(playerId);
	}

	protected void markRewarded(CruciblePlayerReward reward, int insignia) {
		reward.setInsignia(insignia);
		reward.setRewarded();
		runtimeState().put(playerKey(reward.getOwner(), "insignia"), insignia);
		runtimeState().put(playerKey(reward.getOwner(), "rewarded"), true);
	}

	protected void setParticipationState(CruciblePlayerReward reward, int state) {
		reward.setParticipationState(state);
		runtimeState().put(playerKey(reward.getOwner(), "participation"), reward.getParticipationState());
	}

	@Override
	public synchronized boolean onRetailPortalAction(Player player, String action) {
		if (!"JOIN_PLAY".equals(action)) {
			return false;
		}
		CruciblePlayerReward reward = getPlayerReward(player.getObjectId());
		if (reward == null || reward.getParticipationState() == CruciblePlayerReward.PARTICIPATION_FINISHED) {
			return false;
		}
		setParticipationState(reward, CruciblePlayerReward.PARTICIPATION_PLAYING);
		for (Player target : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(target,
					SM_SYSTEM_MESSAGE.STR_MSG_FRIENDLY_MOVE_COMBATAREA_IDARENA(player.getName()));
			sendScore(target);
		}
		return true;
	}

	protected void sendScore(Player player) {
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(instanceReward));
		PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_STAGE_INFO(2, stageType.getId(), stageType.getType()));
	}

	protected static String playerKey(int playerId, String field) {
		return "crucible.player." + playerId + '.' + field;
	}

	private synchronized boolean consumeNpcScore(Npc npc, int points) {
		String stableKey = npc.getSpawn() == null ? null : npc.getSpawn().getStableKey();
		String eventKey = "crucible.score.event."
				+ (stableKey == null || stableKey.isBlank() ? "object." + npc.getObjectId() : stableKey);
		if (runtimeState().getBoolean(eventKey, false)) {
			return true;
		}
		runtimeState().put(eventKey, true);
		if (points == 0) {
			return true;
		}
		for (CruciblePlayerReward reward : instanceReward.getInstanceRewards()) {
			if (!reward.isRewarded()
					&& reward.getParticipationState() == CruciblePlayerReward.PARTICIPATION_PLAYING) {
				reward.addPoints(points);
				runtimeState().put(playerKey(reward.getOwner(), "points"), reward.getPoints());
			}
		}
		for (Player player : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237,
					new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1), points));
			PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(instanceReward));
		}
		return true;
	}
}
