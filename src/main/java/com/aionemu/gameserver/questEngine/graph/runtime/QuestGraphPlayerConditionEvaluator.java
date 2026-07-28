package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerAbyssRankCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerClassCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerEquippedCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerGenderCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerInventoryCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerRaceCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerTitleCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestCompletionCountCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRewardCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.model.ConditionOperation;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 只读评估玩家静态接取资格条件；canonical 任务状态由转换执行器评估。
 * Read-only evaluator for static player eligibility; canonical quest state is evaluated by the transition executor.
 */
@RequiredArgsConstructor
public final class QuestGraphPlayerConditionEvaluator {

	/** 被评估的 PLAYER scope owner。 / PLAYER-scope owner being evaluated. */
	@NonNull
	private final Player player;

	/**
	 * 评估一个类型化条件；玩家 owner 不一致或读取失败时显式 FAILED。
	 * Evaluates one typed condition and explicitly returns FAILED on player-owner mismatch or read failure.
	 */
	public ConditionResult evaluate(ConditionInvocation invocation) {
		if (invocation.event().playerId() != player.getObjectId()) {
			return ConditionResult.FAILED;
		}
		try {
			boolean matched = switch (invocation.condition()) {
				case QuestStatusCondition condition -> throw new IllegalArgumentException("Quest status must be evaluated by the transition executor");
				case QuestRewardCondition condition -> throw new IllegalArgumentException("Quest reward must be evaluated by the transition executor");
				case QuestCompletionCountCondition condition -> throw new IllegalArgumentException(
					"Quest completion count must be evaluated by the transition executor");
				case PlayerLevelCondition condition -> player.getLevel() >= condition.min()
					&& (condition.max() == null || player.getLevel() <= condition.max());
				case PlayerRaceCondition condition -> condition.allowed().contains(player.getRace());
				case PlayerClassCondition condition -> condition.allowed().contains(player.getPlayerClass());
				case PlayerGenderCondition condition -> condition.expected() == player.getGender();
				case PlayerTitleCondition condition -> player.getTitleList().contains(condition.titleId());
				case PlayerAbyssRankCondition condition -> player.getAbyssRank().getRank().getId() >= condition.minimum().getId();
				case PlayerInventoryCondition condition -> compare(player.getInventory().getItemCountByItemId(condition.itemId()),
					condition.operation(), condition.count());
				case PlayerEquippedCondition condition -> player.getEquipment().getEquippedItemIds().contains(condition.itemId());
			};
			return matched ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		} catch (RuntimeException e) {
			return ConditionResult.FAILED;
		}
	}

	/**
	 * 使用旧任务 XML 已有的数值比较语义比较背包数量。
	 * Compares an inventory count with the numeric semantics already used by legacy quest XML.
	 */
	private static boolean compare(long actual, ConditionOperation operation, long expected) {
		return switch (operation) {
			case EQUAL -> actual == expected;
			case GREATER -> actual > expected;
			case GREATER_EQUAL -> actual >= expected;
			case LESSER -> actual < expected;
			case LESSER_EQUAL -> actual <= expected;
			case NOT_EQUAL -> actual != expected;
			case IN, NOT_IN -> throw new IllegalArgumentException("Set operation is invalid for an inventory count");
		};
	}
}
