package com.aionemu.gameserver.model.instance.instancereward;

import com.aionemu.gameserver.model.instance.playerreward.SecretMunitionsFactoryPlayerReward;
import lombok.Getter;
import lombok.Setter;

/**
 * SecretMunitions 工厂奖励，用于副本相关逻辑。
 * Secret Munitions Factory Reward for instance logic.
 */

public class SecretMunitionsFactoryReward extends InstanceReward<SecretMunitionsFactoryPlayerReward> {
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

	public SecretMunitionsFactoryReward(Integer mapId, int instanceId) {
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
