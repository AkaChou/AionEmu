package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Objects;
import java.util.function.Function;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EndQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;

/**
 * 将封闭的任务计时器动作连接到幂等 scheduler、cancel 和客户端协议端点。
 * Connects closed quest-timer actions to idempotent scheduler, cancel, and client-protocol endpoints.
 */
public final class QuestGraphTimerActionAdapter {

	private final Function<StartCommand, ActionResult> starter;
	private final Function<EndCommand, ActionResult> ender;
	private final Function<SyncCommand, ActionResult> synchronizer;

	/**
	 * 创建只接受强类型计时器命令的 bridge。
	 * Creates a bridge that accepts only typed timer commands.
	 */
	public QuestGraphTimerActionAdapter(Function<StartCommand, ActionResult> starter, Function<EndCommand, ActionResult> ender,
		Function<SyncCommand, ActionResult> synchronizer) {
		this.starter = Objects.requireNonNull(starter, "starter");
		this.ender = Objects.requireNonNull(ender, "ender");
		this.synchronizer = Objects.requireNonNull(synchronizer, "synchronizer");
	}

	/**
	 * 在副作用前验证计时器动作与绝对 deadline 计算。
	 * Validates timer actions and absolute-deadline calculation before side effects.
	 */
	public PreflightResult preflight(ActionInvocation invocation) {
		try {
			if (invocation.action() instanceof StartQuestTimerAction start) {
				deadlineAt(invocation, start);
				return PreflightResult.READY;
			}
			return invocation.action() instanceof EndQuestTimerAction ? PreflightResult.READY : PreflightResult.FAILED;
		} catch (RuntimeException e) {
			return PreflightResult.FAILED;
		}
	}

	/**
	 * 执行 scheduler/cancel 或提交后客户端投影，未知动作显式失败。
	 * Executes scheduler/cancel or post-commit client projection and explicitly fails unknown actions.
	 */
	public ActionResult execute(ActionInvocation invocation) {
		try {
			if (invocation.action() instanceof StartQuestTimerAction start) {
				return Objects.requireNonNull(starter.apply(new StartCommand(invocation.questId(), start.timer(),
					deadlineAt(invocation, start), invocation.idempotencyKey())), "timer start result");
			}
			if (invocation.action() instanceof EndQuestTimerAction end) {
				return Objects.requireNonNull(ender.apply(new EndCommand(invocation.questId(), end.timer(), invocation.idempotencyKey())),
					"timer end result");
			}
			if (invocation.action() instanceof SyncQuestTimerAction sync) {
				return Objects.requireNonNull(synchronizer.apply(new SyncCommand(invocation.questId(), sync.timer(), sync.remainingSeconds(),
					invocation.idempotencyKey())), "timer sync result");
			}
			return ActionResult.FAILED;
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
	}

	/** 计算事件锚定的绝对 deadline。 / Calculates the event-anchored absolute deadline. */
	private static long deadlineAt(ActionInvocation invocation, StartQuestTimerAction start) {
		return Math.addExact(invocation.event().occurredAt(), Math.multiplyExact(start.durationSeconds(), 1000));
	}

	/** 表示幂等启动命令。 / Represents an idempotent timer-start command. */
	public record StartCommand(int questId, String timer, long deadlineAt, String idempotencyKey) {
		/** 校验启动命令。 / Validates the start command. */
		public StartCommand {
			if (questId <= 0 || timer == null || timer.isBlank() || deadlineAt <= 0 || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Quest timer start command is invalid");
			}
		}
	}

	/** 表示幂等停止命令。 / Represents an idempotent timer-end command. */
	public record EndCommand(int questId, String timer, String idempotencyKey) {
		/** 校验停止命令。 / Validates the end command. */
		public EndCommand {
			if (questId <= 0 || timer == null || timer.isBlank() || idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Quest timer end command is invalid");
			}
		}
	}

	/** 表示提交后计时器协议投影。 / Represents a post-commit timer protocol projection. */
	public record SyncCommand(int questId, String timer, long remainingSeconds, String idempotencyKey) {
		/** 校验协议投影。 / Validates the protocol projection. */
		public SyncCommand {
			if (questId <= 0 || timer == null || timer.isBlank() || remainingSeconds < 0
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Quest timer sync command is invalid");
			}
		}
	}
}
