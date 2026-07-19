package com.aionemu.gameserver.model.instance.instancereward;

import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.playerreward.BattlegroundPlayerReward;

public final class TreasureIslandReward extends InstanceReward<BattlegroundPlayerReward> {
	private static final int[][] STAGE_SCORES = {
			{ 100, 80, 60, 40, 20, 10 },
			{ 100, 80, 60, 40, 20, 10 },
			{ 200, 160, 140, 120, 100, 80 },
			{ 500, 400, 300, 200, 100, 100 },
			{ 900, 700, 500, 350, 200, 100 }
	};

	private final Map<Integer, Integer> stageMasks = new HashMap<>();
	private final int[] stageArrivals = new int[STAGE_SCORES.length];
	private int elyosPoints;
	private int asmodianPoints;

	public TreasureIslandReward(int mapId, int instanceId) {
		super(mapId, instanceId);
		setInstanceScoreType(InstanceScoreType.PREPARING);
	}

	public synchronized BattlegroundPlayerReward registerPlayer(int objectId, Race race) {
		BattlegroundPlayerReward reward = getPlayerReward(objectId);
		if (reward == null) {
			reward = new BattlegroundPlayerReward(objectId, (byte) 15, race);
			addPlayerReward(reward);
		}
		return reward;
	}

	public synchronized BattlegroundPlayerReward restorePlayer(int objectId, Race race, long joinedAt, int points,
			int stageMask, long logoutAt, long offlineMillis) {
		BattlegroundPlayerReward reward = new BattlegroundPlayerReward(objectId, (byte) 15, race, joinedAt);
		reward.addPoints(points);
		reward.restoreActivity(logoutAt, offlineMillis);
		addPlayerReward(reward);
		stageMasks.put(objectId, stageMask);
		if (race == Race.ELYOS) {
			elyosPoints += points;
		} else if (race == Race.ASMODIANS) {
			asmodianPoints += points;
		}
		return reward;
	}

	public synchronized void restoreStageArrivals(int stage, int arrivals) {
		if (stage < 1 || stage > STAGE_SCORES.length || arrivals < 0) {
			throw new IllegalArgumentException("Invalid Treasure Island stage arrivals");
		}
		stageArrivals[stage - 1] = arrivals;
	}

	/** @return awarded points, or {@code -1} when this player already crossed the stage. */
	public synchronized int registerStage(int objectId, int stage) {
		if (stage < 1 || stage > STAGE_SCORES.length) {
			throw new IllegalArgumentException("Invalid Treasure Island stage " + stage);
		}
		BattlegroundPlayerReward reward = getPlayerReward(objectId);
		if (reward == null) {
			throw new IllegalStateException("Treasure Island player is not registered: " + objectId);
		}
		int bit = 1 << stage - 1;
		int mask = stageMasks.getOrDefault(objectId, 0);
		if ((mask & bit) != 0) {
			return -1;
		}
		stageMasks.put(objectId, mask | bit);
		int arrival = stageArrivals[stage - 1]++;
		int points = arrival < STAGE_SCORES[stage - 1].length ? STAGE_SCORES[stage - 1][arrival] : 0;
		reward.addPoints(points);
		if (reward.getRace() == Race.ELYOS) {
			elyosPoints += points;
		} else if (reward.getRace() == Race.ASMODIANS) {
			asmodianPoints += points;
		}
		return points;
	}

	public synchronized int getPointsByRace(Race race) {
		return race == Race.ELYOS ? elyosPoints : race == Race.ASMODIANS ? asmodianPoints : 0;
	}

	public synchronized int getStageMask(int objectId) {
		return stageMasks.getOrDefault(objectId, 0);
	}

	public synchronized int getStageArrivals(int stage) {
		if (stage < 1 || stage > STAGE_SCORES.length) {
			throw new IllegalArgumentException("Invalid Treasure Island stage " + stage);
		}
		return stageArrivals[stage - 1];
	}

	@Override
	public BattlegroundPlayerReward getPlayerReward(Integer objectId) {
		return (BattlegroundPlayerReward) super.getPlayerReward(objectId);
	}

	@Override
	public synchronized void clear() {
		super.clear();
		stageMasks.clear();
	}
}
