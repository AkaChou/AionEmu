package com.aionemu.gameserver.model.instance.instancereward;

import com.aionemu.gameserver.model.instance.playerreward.ShugoEmperorVaultPlayerReward;

/**
 * 术古 EmperorVault 奖励，用于副本相关逻辑。
 * Shugo Emperor Vault Reward for instance logic.
 */

public class ShugoEmperorVaultReward extends InstanceReward<ShugoEmperorVaultPlayerReward> {
	private int points;
	private int npcKills;
	private int rank = 7;

	public ShugoEmperorVaultReward(Integer mapId, int instanceId) {
		super(mapId, instanceId);
	}

	/** 添加点。 / Adds points. */
	public void addPoints(int points) {
		this.points += points;
	}

	/** 获取点。 / Returns the points. */
	public int getPoints() {
		return points;
	}

	/** 添加 npc kill / Adds npc kill */
	public void addNpcKill() {
		npcKills++;
	}

	/** 返回 npc kills / Returns the npc kills */
	public int getNpcKills() {
		return npcKills;
	}

	/** 设置军阶。 / Sets the rank. */
	public void setRank(int rank) {
		this.rank = rank;
	}

	/** 获取军阶。 / Returns the rank. */
	public int getRank() {
		return rank;
	}
}
