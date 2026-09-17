package com.aionemu.gameserver.model.instance.instancereward;

import com.aionemu.gameserver.model.instance.playerreward.FissureOfOblivionPlayerReward;
import lombok.Getter;
import lombok.Setter;

/**
 * FissureOfOblivion 奖励，用于副本相关逻辑。
 * Fissure Of Oblivion Reward for instance logic.
 */

@Getter
@Setter
public class FissureOfOblivionReward extends InstanceReward<FissureOfOblivionPlayerReward> {
	/** 获取点。 / Returns the points. */
	private int points;
	/** 返回 npc kills / Returns the npc kills */
	private int npcKills;
	/** 设置军阶。 / Sets the rank. */
	private int rank = 7;

	public FissureOfOblivionReward(Integer mapId, int instanceId) {
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
