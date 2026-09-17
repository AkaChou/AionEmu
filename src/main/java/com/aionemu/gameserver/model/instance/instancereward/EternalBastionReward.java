package com.aionemu.gameserver.model.instance.instancereward;

import com.aionemu.gameserver.model.instance.playerreward.EternalBastionPlayerReward;
import lombok.Getter;
import lombok.Setter;

/**
 * EternalBastion 奖励，用于副本相关逻辑。
 * Eternal Bastion Reward for instance logic.
 */

@Getter
@Setter
public class EternalBastionReward extends InstanceReward<EternalBastionPlayerReward> {
	/** 获取点。 / Returns the points. */
	private int points;
	/** 返回 npc kills / Returns the npc kills */
	private int npcKills;
	/** 设置军阶。 / Sets the rank. */
	private int rank = 7;

	public EternalBastionReward(Integer mapId, int instanceId) {
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
