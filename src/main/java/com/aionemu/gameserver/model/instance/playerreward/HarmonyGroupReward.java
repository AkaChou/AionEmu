package com.aionemu.gameserver.model.instance.playerreward;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.List;

import com.aionemu.gameserver.model.autogroup.AGPlayer;
import com.aionemu.gameserver.utils.idfactory.IDFactory;

/**
 * Harmony 队伍奖励，用于副本相关逻辑。
 * Harmony Group Reward for instance logic.
 */

public class HarmonyGroupReward extends PvPArenaPlayerReward {
	private int id;
	private List<AGPlayer> players;

	public HarmonyGroupReward(Integer object, int timeBonus, byte buffId, List<AGPlayer> players) {
		super(object, timeBonus, buffId);
		this.players = players;
		id = GameWorldBootstrapServices.idFactory().nextId();
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
