package com.aionemu.gameserver.model.instance.playerreward;

/**
 * 副本玩家奖励模型。
 * Instance Player Reward model.
 */

public class InstancePlayerReward {
	private int points;
	private int playerPvPKills;
	private int playerMonsterKills;
	protected Integer object;

	public InstancePlayerReward(Integer object) {
		this.object = object;
	}

	/** 返回所有者 / Returns the owner*/
	public Integer getOwner() {
		return object;
	}

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return points;
	}

	/** 返回 pv p kills / Returns the pv p kills */
	public int getPvPKills() {
		return playerPvPKills;
	}

	/** 返回 monster kills / Returns the monster kills */
	public int getMonsterKills() {
		return playerMonsterKills;
	}

	/** 添加点。 / Adds points. */
	public void addPoints(int points) {
		this.points += points;
		if (this.points < 0) {
			this.points = 0;
		}
	}

	/** 添加 pv p kill to player / Adds pv p kill to player */
	public void addPvPKillToPlayer() {
		playerPvPKills++;
	}

	/** 添加 monster kill to player / Adds monster kill to player */
	public void addMonsterKillToPlayer() {
		playerMonsterKills++;
	}

	public void restore(int points, int pvpKills, int monsterKills) {
		this.points = Math.max(0, points);
		this.playerPvPKills = Math.max(0, pvpKills);
		this.playerMonsterKills = Math.max(0, monsterKills);
	}
}
