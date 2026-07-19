package com.aionemu.gameserver.model.instance.playerreward;

import java.util.List;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.autogroup.AGPlayer;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;

/**
 * Harmony 队伍奖励，用于副本相关逻辑。
 * Harmony Group Reward for instance logic.
 */

public class HarmonyGroupReward extends InstancePlayerReward {
	private final int id;
	private final List<AGPlayer> players;
	private final int scoreFloor;

	public HarmonyGroupReward(Integer object, Row arenaRow, List<AGPlayer> players) {
		super(object);
		scoreFloor = arenaRow.requiredInt("score_limit_bottom");
		super.addPoints(arenaRow.requiredInt("basescore_enter"));
		this.players = players;
		id = GameWorldBootstrapServices.idFactory().nextId();
	}

	@Override
	public void addPoints(int points) {
		super.addPoints(points);
		if (getPoints() < scoreFloor) {
			super.addPoints(scoreFloor - getPoints());
		}
	}

	/** 返回 ag players / Returns the ag players */
	public List<AGPlayer> getAGPlayers() {
		return players;
	}

	/** 包含玩家 / Contain Player */
	public boolean containPlayer(Integer object) {
		for (AGPlayer agp : players) {
			if (agp.getObjectId().equals(object)) {
				return true;
			}
		}
		return false;
	}

	/** 返回 ag player / Returns the ag player */
	public AGPlayer getAGPlayer(Integer object) {
		for (AGPlayer agp : players) {
			if (agp.getObjectId().equals(object)) {
				return agp;
			}
		}
		return null;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}
}
