package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;

/** Resolves typed reward amount semantics against the authoritative player rates. */
final class QuestRewardAmounts {
	private QuestRewardAmounts() {
	}

	static long resolve(Player player, QuestAction.GrantReward reward) {
		if (reward.amountMode() == QuestRewardAmountMode.EXACT) {
			return reward.amount();
		}
		return switch (reward.rewardKind()) {
			case GOLD, KINAH -> (long) (player.getRates().getQuestKinahRate() * reward.amount());
			case AP -> (long) (player.getRates().getQuestApRate() * reward.amount());
			case GP -> (long) (player.getRates().getQuestGpRate() * reward.amount());
			// RewardType.QUEST resolves the configured XP rate inside PlayerCommonData;
			// EXACT uses RewardType.EXACT and bypasses that source rate.
			case EXP -> reward.amount();
			default -> throw new IllegalArgumentException(
				"QUEST_BASE is unsupported for reward kind " + reward.rewardKind());
		};
	}
}
