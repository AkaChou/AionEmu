package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Objects;

/** One ordered legacy-compatible reward alternative. */
public record QuestRewardGroup(List<QuestReward> rewards) {
	public QuestRewardGroup {
		rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
	}
}
