package com.aionemu.gameserver.model.instance.instancereward;

import lombok.Getter;
import lombok.Setter;

/**
 * DarkPoeta 奖励，用于副本相关逻辑。
 * Dark Poeta Reward for instance logic.
 */

@SuppressWarnings("rawtypes")
public class DarkPoetaReward extends InstanceReward {
	/** 获取点。 / Returns the points. */
	@Getter
	private int points;
	/** 设置军阶。 / Sets the rank. */
	@Getter
	@Setter
	private int rank = 7;
	/** 返回 npc kills / Returns the npc kills */
	@Getter
	private int npcKills;
	/** 返回 gather collections / Returns the gather collections */
	@Getter
	private int gatherCollections;

	public DarkPoetaReward(Integer mapId, int instanceId) {
		super(mapId, instanceId);
	}

	/** 添加点。 / Adds points. */
	public void addPoints(int points) {
		this.points += points;
	}

	/** 添加 npc kill / Adds npc kill */
	public void addNpcKill() {
		npcKills++;
	}

	/** 添加 gather collection / Adds gather collection */
	public void addGatherCollection() {
		gatherCollections++;
	}
}
