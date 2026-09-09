package com.aionemu.gameserver.model.instance.instancereward;

import com.aionemu.gameserver.model.instance.playerreward.IDEventDefPlayerReward;
import lombok.Getter;
import lombok.Setter;

/**
 * ID 活动 Def 奖励，用于副本相关逻辑。
 * ID Event Def Reward for instance logic.
 */

public class IDEventDefReward extends InstanceReward<IDEventDefPlayerReward> {
	/** 获取点。 / Returns the points. */
	@Getter
	private int points;
	/** 返回 npc kills / Returns the npc kills */
	@Getter
	private int npcKills;
	/** 设置军阶。 / Sets the rank. */
	@Getter
	@Setter
	private int rank = 7;

	public IDEventDefReward(Integer mapId, int instanceId) {
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
}
