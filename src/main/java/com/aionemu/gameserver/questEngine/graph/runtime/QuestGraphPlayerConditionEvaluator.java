package com.aionemu.gameserver.questEngine.graph.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerAbyssRankCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PackedCounterCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.KillVictimLevelDeltaCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.InvasionWorldActiveCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerClassCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerEquippedCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerGenderCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerInventoryCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerRaceCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerTitleCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestCompletionCountCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestCollectItemsCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRepeatAvailableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRewardCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestVariableCondition;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.KillInWorldEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;
import com.aionemu.gameserver.questEngine.model.ConditionOperation;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.QuestService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * 只读评估玩家属性及绑定服务端事件快照的条件；canonical 任务状态由转换执行器评估。
 * Read-only evaluator for player attributes and server-event-bound conditions; canonical quest state is evaluated by the transition executor.
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
				case QuestVariableCondition condition -> throw new IllegalArgumentException("Quest variable must be evaluated by the transition executor");
				case PackedCounterCondition condition -> throw new IllegalArgumentException("Packed counter must be evaluated by the transition executor");
				case InvasionWorldActiveCondition condition -> matchesInvasionWorld(condition, invocation);
				case QuestRepeatAvailableCondition condition -> throw new IllegalArgumentException(
					"Quest repeat eligibility must be evaluated by the transition executor");
				case QuestRewardCondition condition -> throw new IllegalArgumentException("Quest reward must be evaluated by the transition executor");
				case QuestCompletionCountCondition condition -> throw new IllegalArgumentException(
					"Quest completion count must be evaluated by the transition executor");
				case PlayerLevelCondition condition -> player.getLevel() >= condition.min()
					&& (condition.max() == null || player.getLevel() <= condition.max());
				case KillVictimLevelDeltaCondition condition -> matchesKillVictimLevelDelta(condition, invocation);
				case PlayerRaceCondition condition -> condition.allowed().contains(player.getRace());
				case PlayerClassCondition condition -> condition.allowed().contains(player.getPlayerClass());
				case PlayerGenderCondition condition -> condition.expected() == player.getGender();
				case PlayerTitleCondition condition -> player.getTitleList().contains(condition.titleId());
				case PlayerAbyssRankCondition condition -> player.getAbyssRank().getRank().getId() >= condition.minimum().getId();
				case PlayerInventoryCondition condition -> compare(player.getInventory().getItemCountByItemId(condition.itemId()),
					condition.operation(), condition.count());
				case PlayerEquippedCondition condition -> player.getEquipment().getEquippedItemIds().contains(condition.itemId());
				case QuestCollectItemsCondition condition -> QuestService.collectItemCheck(
					new QuestEnv(null, player, invocation.questId(), 0), false);
			};
			return matched ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		} catch (RuntimeException e) {
			return ConditionResult.FAILED;
		}
	}

	/** 使用持久化 WORLD_ENTERED 快照校验世界与服务端入侵访问凭据。 / Validates the world and server invasion-access authority from the persisted WORLD_ENTERED snapshot. */
	private boolean matchesInvasionWorld(InvasionWorldActiveCondition condition, ConditionInvocation invocation) {
		if (!(invocation.event() instanceof WorldEnteredEvent event)) {
			throw new IllegalArgumentException("Invasion world condition requires WORLD_ENTERED");
		}
		return event.worldId() == condition.worldId() && event.invasionAccessActive();
	}

	/**
	 * 使用 KILL_IN_WORLD 的受害者等级快照比较当前玩家等级差；错误事件类型失败关闭。
	 * Compares the current player level against the KILL_IN_WORLD victim-level snapshot and fails closed for other events.
	 */
	private boolean matchesKillVictimLevelDelta(KillVictimLevelDeltaCondition condition, ConditionInvocation invocation) {
		if (!(invocation.event() instanceof KillInWorldEvent event)) {
			throw new IllegalArgumentException("Kill-victim level delta requires KILL_IN_WORLD");
		}
		int delta = player.getLevel() - event.victimLevel();
		return (condition.min() == null || delta >= condition.min()) && (condition.max() == null || delta <= condition.max());
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
