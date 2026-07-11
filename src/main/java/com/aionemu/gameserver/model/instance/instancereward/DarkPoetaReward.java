package com.aionemu.gameserver.model.instance.instancereward;

/**
 * DarkPoeta 奖励，用于副本相关逻辑。
 * Dark Poeta Reward for instance logic.
 */

@SuppressWarnings("rawtypes")
public class DarkPoetaReward extends InstanceReward {
	private int points;
	private int rank = 7;
	private int npcKills;
	private int gatherCollections;

	public DarkPoetaReward(Integer mapId, int instanceId) {
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

	/** 添加 gather collection / Adds gather collection */
	public void addGatherCollection() {
		gatherCollections++;
	}

	/** 返回 gather collections / Returns the gather collections */
	public int getGatherCollections() {
		return gatherCollections;
	}
}
