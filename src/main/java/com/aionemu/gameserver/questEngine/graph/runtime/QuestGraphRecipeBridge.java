package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.Function;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DeleteRecipeAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.LearnRecipeAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.NotifyRecipeRejectionAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RecipeLearnableCondition;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;

/**
 * 将配方资格、持久化学习/删除和拒绝协议连接到封闭 typed 端口。
 * Connects recipe eligibility, durable learn/delete operations, and rejection protocol to closed typed ports.
 */
public final class QuestGraphRecipeBridge {

	private final int playerId;
	private final Function<RecipeEligibilityQuery, ConditionResult> eligibility;
	private final Function<RecipeCommand, PreflightResult> preflight;
	private final Function<RecipeCommand, ActionResult> executor;

	/**
	 * 创建绑定单一玩家且拒绝未知命令的 recipe bridge。
	 * Creates a recipe bridge bound to one player that rejects unknown commands.
	 */
	public QuestGraphRecipeBridge(int playerId, Function<RecipeEligibilityQuery, ConditionResult> eligibility,
			Function<RecipeCommand, PreflightResult> preflight, Function<RecipeCommand, ActionResult> executor) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Recipe bridge player id is invalid");
		}
		this.playerId = playerId;
		this.eligibility = Objects.requireNonNull(eligibility, "recipe eligibility");
		this.preflight = Objects.requireNonNull(preflight, "recipe preflight");
		this.executor = Objects.requireNonNull(executor, "recipe executor");
	}

	/** 评估引用闭合配方的期望可学习性；owner mismatch、未知条件或异常显式失败。 / Evaluates expected learnability for a reference-closed recipe; owner mismatch, unknown conditions, and exceptions fail explicitly. */
	public ConditionResult evaluate(ConditionInvocation invocation) {
		try {
			if (!validOwner(invocation) || !(invocation.condition() instanceof RecipeLearnableCondition condition)) {
				return ConditionResult.FAILED;
			}
			ConditionResult actual = Objects.requireNonNull(
				eligibility.apply(new RecipeEligibilityQuery(invocation.questId(), playerId, condition.recipeId())),
				"recipe eligibility result");
			if (actual == ConditionResult.FAILED) {
				return actual;
			}
			boolean learnable = actual == ConditionResult.MATCHED;
			return learnable == condition.expected() ? ConditionResult.MATCHED : ConditionResult.NOT_MATCHED;
		} catch (RuntimeException e) {
			return ConditionResult.FAILED;
		}
	}

	/** 在 PREPARED 前校验学习或删除命令；协议拒绝通知不参与 required-action 预检。 / Preflights learn or delete commands before PREPARED; rejection protocol does not participate in required-action preflight. */
	public PreflightResult preflight(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation) || invocation.action() instanceof NotifyRecipeRejectionAction) {
				return PreflightResult.FAILED;
			}
			return Objects.requireNonNull(preflight.apply(command(invocation)), "recipe preflight result");
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/** 执行幂等学习/删除或提交后拒绝协议；owner mismatch、未知动作或异常显式失败。 / Executes idempotent learn/delete or post-commit rejection protocol; owner mismatch, unknown actions, and exceptions fail explicitly. */
	public ActionResult execute(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation)) {
				return ActionResult.FAILED;
			}
			return Objects.requireNonNull(executor.apply(command(invocation)), "recipe action result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** 校验条件事件属于当前玩家。 / Validates that a condition event belongs to this player. */
	private boolean validOwner(ConditionInvocation invocation) {
		return invocation != null && invocation.event().playerId() == playerId;
	}

	/** 校验动作事件属于当前玩家。 / Validates that an action event belongs to this player. */
	private boolean validOwner(ActionInvocation invocation) {
		return invocation != null && invocation.event().playerId() == playerId;
	}

	/** 将封闭 IR 动作转换为唯一 typed recipe 命令。 / Converts a closed IR action into one typed recipe command. */
	private RecipeCommand command(ActionInvocation invocation) {
		if (invocation.action() instanceof LearnRecipeAction action) {
			return new LearnRecipeCommand(invocation.questId(), playerId, action.recipeId(), invocation.idempotencyKey());
		}
		if (invocation.action() instanceof DeleteRecipeAction action) {
			return new DeleteRecipeCommand(invocation.questId(), playerId, action.recipeId(), invocation.idempotencyKey());
		}
		if (invocation.action() instanceof NotifyRecipeRejectionAction action) {
			return new NotifyRecipeRejectionCommand(invocation.questId(), playerId, action.recipeId(), invocation.idempotencyKey());
		}
		throw new IllegalArgumentException("Unsupported recipe action " + invocation.action().type());
	}

	/** 表示只读 recipe eligibility 查询。 / Represents a read-only recipe-eligibility query. */
	public record RecipeEligibilityQuery(int questId, int playerId, int recipeId) {
		/** 校验 owner 和配方引用。 / Validates owner and recipe reference. */
		public RecipeEligibilityQuery {
			validate(questId, playerId, recipeId, "query");
		}
	}

	/** 定义 recipe bridge 接受的封闭命令集合。 / Defines the closed command set accepted by the recipe bridge. */
	public sealed interface RecipeCommand permits LearnRecipeCommand, DeleteRecipeCommand, NotifyRecipeRejectionCommand {
		/** 返回稳定幂等键。 / Returns the stable idempotency key. */
		String idempotencyKey();
	}

	/** 表示持久化学习临时工单配方。 / Represents durably learning a temporary work-order recipe. */
	public record LearnRecipeCommand(int questId, int playerId, int recipeId, String idempotencyKey) implements RecipeCommand {
		/** 校验 owner、配方引用和幂等键。 / Validates owner, recipe reference, and idempotency key. */
		public LearnRecipeCommand {
			validate(questId, playerId, recipeId, idempotencyKey);
		}
	}

	/** 表示幂等删除临时工单配方。 / Represents idempotently deleting a temporary work-order recipe. */
	public record DeleteRecipeCommand(int questId, int playerId, int recipeId, String idempotencyKey) implements RecipeCommand {
		/** 校验 owner、配方引用和幂等键。 / Validates owner, recipe reference, and idempotency key. */
		public DeleteRecipeCommand {
			validate(questId, playerId, recipeId, idempotencyKey);
		}
	}

	/** 表示把资格拒绝原因投影到客户端。 / Represents projecting an eligibility rejection reason to the client. */
	public record NotifyRecipeRejectionCommand(int questId, int playerId, int recipeId, String idempotencyKey) implements RecipeCommand {
		/** 校验 owner、配方引用和幂等键。 / Validates owner, recipe reference, and idempotency key. */
		public NotifyRecipeRejectionCommand {
			validate(questId, playerId, recipeId, idempotencyKey);
		}
	}

	/** 校验 recipe bridge 的公共 owner、引用和稳定键。 / Validates common recipe-bridge owner, reference, and stable-key fields. */
	private static void validate(int questId, int playerId, int recipeId, String stableKey) {
		if (questId <= 0 || playerId <= 0 || recipeId <= 0 || stableKey == null || stableKey.isBlank()) {
			throw new IllegalArgumentException("Recipe bridge input is invalid");
		}
	}
}
