package com.aionemu.gameserver.model.instance.instancereward;

import lombok.Getter;
import lombok.Setter;

/**
 * DarkPoeta 奖励，用于副本相关逻辑。
 * Dark Poeta Reward for instance logic.
 */

@Getter
@Setter
@SuppressWarnings("rawtypes")
public class DarkPoetaReward extends InstanceReward {
	/** 获取点。 / Returns the points. */
	private int points;
	/** 设置军阶。 / Sets the rank. */
	private int rank = 7;
	/** 返回 npc kills / Returns the npc kills */
	private int npcKills;
	/** 返回 gather collections / Returns the gather collections */
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
