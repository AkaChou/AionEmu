package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/**
 * 一条有序的旧版兼容奖励备选方案。
 * One ordered legacy-compatible reward alternative.
 */
public record QuestRewardGroup(List<QuestReward> rewards) {
	public QuestRewardGroup {
		rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
	}
}
