package com.aionemu.gameserver.model;

/**
 * 军阶计数模型。
 * Rank Count model.
 */

public class RankCount {
	private int playerId;
	private int ap;
	private int gp;
	private Race race;

	public RankCount(int playerId, int ap, int gp, Race race) {
		this.playerId = playerId;
		this.ap = ap;
		this.gp = gp;
		this.race = race;
	}

	/** 返回玩家 ID / Returns the player id */
	public int getPlayerId() {
		return playerId;
	}

	/** 获取玩家欧比斯点数。 / Returns the player ap. */
	public int getPlayerAP() {
		return ap;
	}

	/** 获取玩家荣耀点数。 / Returns the player gp. */
	public int getPlayerGP() {
		return gp;
	}

	/** 获取玩家种族。 / Returns the player race. */
	public Race getPlayerRace() {
		return race;
	}
}
