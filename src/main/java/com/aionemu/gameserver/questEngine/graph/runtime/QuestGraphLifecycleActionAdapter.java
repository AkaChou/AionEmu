package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AbandonQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.FinishQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveCollectedItemsAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEventQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

/**
 * 将封闭任务生命周期动作连接到具备幂等和 cleanup 合同的外部 owner。
 * Connects closed quest-lifecycle actions to an external owner with idempotency and cleanup contracts.
 */
public final class QuestGraphLifecycleActionAdapter {

	private final int playerId;
	private final Function<LifecycleCommand, PreflightResult> preflight;
	private final Function<LifecycleCommand, ActionResult> executor;

	/**
	 * 创建只接受封闭 lifecycle 命令的 typed bridge。
	 * Creates a typed bridge that accepts only closed lifecycle commands.
	 */
	public QuestGraphLifecycleActionAdapter(int playerId, Function<LifecycleCommand, PreflightResult> preflight,
			Function<LifecycleCommand, ActionResult> executor) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Lifecycle adapter player id is invalid");
		}
		this.playerId = playerId;
		this.preflight = Objects.requireNonNull(preflight, "lifecycle preflight");
		this.executor = Objects.requireNonNull(executor, "lifecycle executor");
	}

	/**
	 * 在 PREPARED 前验证 owner、状态、奖励选择和 cleanup 输入。
	 * Validates owner, status, reward selection, and cleanup input before PREPARED.
	 */
	public PreflightResult preflight(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation)) {
				return PreflightResult.FAILED;
			}
			return Objects.requireNonNull(preflight.apply(command(invocation)), "lifecycle preflight result");
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/**
	 * 执行幂等 lifecycle 命令；未知动作、错误 owner 和异常显式失败。
	 * Executes an idempotent lifecycle command; unknown actions, wrong owners, and exceptions fail explicitly.
	 */
	public ActionResult execute(ActionInvocation invocation) {
		try {
			if (!validOwner(invocation)) {
				return ActionResult.FAILED;
			}
			return Objects.requireNonNull(executor.apply(command(invocation)), "lifecycle action result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** 校验事件玩家与 adapter owner 一致。 / Validates that the event player matches the adapter owner. */
	private boolean validOwner(ActionInvocation invocation) {
		return invocation != null && invocation.event().playerId() == playerId;
	}

	/** 将封闭 IR 动作转换为唯一 typed lifecycle 命令。 / Converts a closed IR action to its unique typed lifecycle command. */
	private static LifecycleCommand command(ActionInvocation invocation) {
		if (invocation.action() instanceof StartQuestAction start) {
			return new StartCommand(invocation.questId(), invocation.questStatus(), invocation.idempotencyKey());
		}
		if (invocation.action() instanceof StartEventQuestAction startEvent) {
			return new EventStartCommand(invocation.questId(), startEvent.targetQuestId(), startEvent.status(), invocation.idempotencyKey());
		}
		if (invocation.action() instanceof RemoveCollectedItemsAction) {
			return new CollectedItemsCleanupCommand(invocation.questId(), invocation.cleanupLeases(), invocation.idempotencyKey());
		}
		if (invocation.action() instanceof FinishQuestAction finish) {
			return new SettlementCommand(invocation.questId(), finish.rewardIndex(), invocation.questStatus(),
				invocation.repeatDeadlineResolution(), invocation.cleanupLeases(), invocation.idempotencyKey());
		}
		if (invocation.action() instanceof AbandonQuestAction) {
			return new AbandonCommand(invocation.questId(), invocation.questStatus(), invocation.cleanupLeases(), invocation.idempotencyKey());
		}
		throw new IllegalArgumentException("Unsupported lifecycle action " + invocation.action().type());
	}

	/** 定义 lifecycle bridge 接受的封闭命令集合。 / Defines the closed command set accepted by the lifecycle bridge. */
	public sealed interface LifecycleCommand permits StartCommand, EventStartCommand, CollectedItemsCleanupCommand, SettlementCommand,
		AbandonCommand {
		/** 返回稳定幂等键。 / Returns the stable idempotency key. */
		String idempotencyKey();
	}

	/** 表示当前 owner 的标准或活动启动。 / Represents a standard or event start for the current owner. */
	public record StartCommand(int questId, QuestStatus previousStatus, String idempotencyKey)
			implements LifecycleCommand {
		/** 校验启动命令。 / Validates the start command. */
		public StartCommand {
			validateCommon(questId, idempotencyKey);
			Objects.requireNonNull(previousStatus, "previous status");
		}
	}

	/** 表示以显式状态启动活动任务 owner。 / Represents starting an event-quest owner with an explicit status. */
	public record EventStartCommand(int sourceQuestId, int targetQuestId, QuestStatus status, String idempotencyKey)
			implements LifecycleCommand {
		/** 校验活动任务启动命令。 / Validates the event-quest start command. */
		public EventStartCommand {
			validateCommon(sourceQuestId, idempotencyKey);
			if (targetQuestId <= 0 || status == null) {
				throw new IllegalArgumentException("Event quest start command is invalid");
			}
		}
	}

	/** 表示 quest_data 交付物品的幂等清理。 / Represents idempotent cleanup of quest-data delivery items. */
	public record CollectedItemsCleanupCommand(int questId, Map<String, CleanupLease> cleanupLeases, String idempotencyKey)
			implements LifecycleCommand {
		/** 校验交付物品清理命令。 / Validates the collected-item cleanup command. */
		public CollectedItemsCleanupCommand {
			validateCommon(questId, idempotencyKey);
			cleanupLeases = immutableLeases(cleanupLeases);
		}
	}

	/** 表示奖励选择、repeat deadline 和全部资源清理的一次幂等结算。 / Represents one idempotent reward settlement with repeat deadline and resource cleanup. */
	public record SettlementCommand(int questId, int rewardIndex, QuestStatus previousStatus,
			RepeatDeadlineResolution repeatDeadlineResolution, Map<String, CleanupLease> cleanupLeases, String idempotencyKey)
			implements LifecycleCommand {
		/** 校验结算命令。 / Validates the settlement command. */
		public SettlementCommand {
			validateCommon(questId, idempotencyKey);
			if (rewardIndex < 0 || previousStatus != QuestStatus.REWARD) {
				throw new IllegalArgumentException("Quest settlement command is invalid");
			}
			Objects.requireNonNull(repeatDeadlineResolution, "repeat deadline resolution");
			cleanupLeases = immutableLeases(cleanupLeases);
		}
	}

	/** 表示当前 owner 的幂等放弃和全部资源清理。 / Represents idempotent abandonment and complete resource cleanup for the current owner. */
	public record AbandonCommand(int questId, QuestStatus previousStatus, Map<String, CleanupLease> cleanupLeases, String idempotencyKey)
			implements LifecycleCommand {
		/** 校验放弃命令。 / Validates the abandonment command. */
		public AbandonCommand {
			validateCommon(questId, idempotencyKey);
			if (previousStatus == null || previousStatus == QuestStatus.NONE || previousStatus == QuestStatus.COMPLETE) {
				throw new IllegalArgumentException("Quest abandonment status is invalid");
			}
			cleanupLeases = immutableLeases(cleanupLeases);
		}
	}

	/** 校验 owner 与幂等键。 / Validates the owner and idempotency key. */
	private static void validateCommon(int questId, String idempotencyKey) {
		if (questId <= 0 || idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("Lifecycle command owner/idempotency key is invalid");
		}
	}

	/** 复制并校验 cleanup ledger。 / Copies and validates the cleanup ledger. */
	private static Map<String, CleanupLease> immutableLeases(Map<String, CleanupLease> cleanupLeases) {
		return Map.copyOf(Objects.requireNonNull(cleanupLeases, "cleanup leases"));
	}
}
