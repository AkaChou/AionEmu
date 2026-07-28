package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerClassCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerGenderCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerRaceCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 只读评估 canonical 任务状态和玩家静态接取资格条件。
 * Read-only evaluator for canonical quest status and static player start-eligibility conditions.
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
				case QuestStatusCondition condition -> condition.expected() == invocation.questStatus();
				case PlayerLevelCondition condition -> player.getLevel() >= condition.min()
					&& (condition.max() == null || player.getLevel() <= condition.max());
				case PlayerRaceCondition condition -> condition.allowed().contains(player.getRace());
				case PlayerClassCondition condition -> condition.allowed().contains(player.getPlayerClass());
				case PlayerGenderCondition condition -> condition.expected() == player.getGender();
			};
			return matched ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		} catch (RuntimeException e) {
			return ConditionResult.FAILED;
		}
	}
}
