package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ScheduleItemUseDialogAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 将 schedule-item-use-dialog 提交后协议接到 3 秒物品使用动画与延迟对话页。
 * Connects the schedule-item-use-dialog post-commit protocol to the item-use animation and delayed dialog page.
 */
public final class QuestGraphItemUseAnimationAdapter {

	private final int playerId;
	private final Function<StartCommand, ActionResult> starter;
	private final BiFunction<CompletionCommand, Runnable, ScheduleResult> scheduler;
	private final Function<CompletionCommand, ActionResult> completer;
	private final Function<CompletionCommand, ActionResult> completionRetry;
	private final Set<String> acceptedKeys = new HashSet<>();
	private final Map<String, PendingSchedule> pendingSchedules = new HashMap<>();

	/**
	 * 创建在线玩家 adapter：广播动画并通过注入的 scheduler 延迟完成。
	 * Creates an online-player adapter that broadcasts animation and delays completion via an injected scheduler.
	 */
	public QuestGraphItemUseAnimationAdapter(Player player,
			BiFunction<CompletionCommand, Runnable, ScheduleResult> scheduler,
			Function<CompletionCommand, ActionResult> completer,
			Function<CompletionCommand, ActionResult> completionRetry) {
		this(requirePlayer(player).getObjectId(), command -> startAnimation(player, command), scheduler, completer, completionRetry);
	}

	/** 创建可注入端点的聚焦测试 adapter。 / Creates a focused-test adapter with injectable endpoints. */
	QuestGraphItemUseAnimationAdapter(int playerId, Function<StartCommand, ActionResult> starter,
			BiFunction<CompletionCommand, Runnable, ScheduleResult> scheduler,
			Function<CompletionCommand, ActionResult> completer) {
		this(playerId, starter, scheduler, completer, command -> ActionResult.FAILED);
	}

	/** 创建带显式 completion retry/outbox 的聚焦测试 adapter。 / Creates a focused-test adapter with an explicit completion retry/outbox. */
	QuestGraphItemUseAnimationAdapter(int playerId, Function<StartCommand, ActionResult> starter,
			BiFunction<CompletionCommand, Runnable, ScheduleResult> scheduler,
			Function<CompletionCommand, ActionResult> completer, Function<CompletionCommand, ActionResult> completionRetry) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Item-use animation adapter player id is invalid");
		}
		this.playerId = playerId;
		this.starter = Objects.requireNonNull(starter, "starter");
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
		this.completer = Objects.requireNonNull(completer, "completer");
		this.completionRetry = Objects.requireNonNull(completionRetry, "completionRetry");
	}

	/**
	 * 启动动画并调度延迟对话；稳定幂等键已接受时不重复调度，错误事件/owner 显式失败。
	 * Starts animation and schedules the delayed dialog; an accepted stable key is not rescheduled, wrong event/owner fails closed.
	 */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		if (invocation == null || invocation.event().playerId() != playerId
				|| !(invocation.action() instanceof ScheduleItemUseDialogAction action)
				|| !(invocation.event() instanceof ItemUseEvent event)) {
			return ActionResult.FAILED;
		}
		if (acceptedKeys.contains(invocation.idempotencyKey())) {
			return ActionResult.ALREADY_APPLIED;
		}
		StartCommand start;
		CompletionCommand completion;
		try {
			start = new StartCommand(invocation.questId(), playerId, event.itemId(), event.itemObjectId(), action.durationMs(),
				invocation.idempotencyKey());
			completion = new CompletionCommand(invocation.questId(), playerId, event.itemId(), event.itemObjectId(), action.dialogId(),
				invocation.idempotencyKey());
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		ActionResult started;
		try {
			started = Objects.requireNonNull(starter.apply(start), "item-use animation start result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		if (started != ActionResult.APPLIED && started != ActionResult.ALREADY_APPLIED) {
			return ActionResult.FAILED;
		}
		PendingSchedule pending = new PendingSchedule();
		pendingSchedules.put(completion.idempotencyKey(), pending);
		Runnable finish = () -> finish(completion, pending);
		ScheduleResult scheduled;
		try {
			scheduled = Objects.requireNonNull(scheduler.apply(completion, finish), "item-use schedule result");
		} catch (RuntimeException e) {
			pendingSchedules.remove(completion.idempotencyKey(), pending);
			return ActionResult.FAILED;
		}
		if (!accepted(scheduled.result())) {
			pendingSchedules.remove(completion.idempotencyKey(), pending);
			cancel(scheduled.cancellation());
			return ActionResult.FAILED;
		}
		pending.cancellation = scheduled.cancellation();
		pending.active = true;
		// 调度成功即占用幂等键，避免重复广播动画；完成回调只负责对话投影。
		// Claiming the key on schedule prevents animation replay; the completion callback only projects the dialog.
		acceptedKeys.add(completion.idempotencyKey());
		if (pending.fired) {
			complete(completion, pending);
		}
		return scheduled.result();
	}

	/** 取消未完成调度并清理会话幂等集合。 / Cancels outstanding schedules and clears the session idempotency set. */
	public synchronized ActionResult clear() {
		ActionResult result = ActionResult.ALREADY_APPLIED;
		for (Iterator<Map.Entry<String, PendingSchedule>> iterator = pendingSchedules.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, PendingSchedule> entry = iterator.next();
			if (cancel(entry.getValue().cancellation)) {
				iterator.remove();
				if (result != ActionResult.FAILED) {
					result = ActionResult.APPLIED;
				}
			} else {
				result = ActionResult.FAILED;
			}
		}
		acceptedKeys.retainAll(pendingSchedules.keySet());
		return result;
	}

	/** 返回已接受键数量，仅用于确定性测试与审计。 / Returns the accepted-key count only for deterministic tests and audit. */
	public synchronized int size() {
		return acceptedKeys.size();
	}

	/** 在 scheduler 接管后执行 completion；同步 scheduler 会先标记 fired，再由 execute 激活。 / Completes after scheduler acceptance; synchronous schedulers mark fired until execute activates them. */
	private void finish(CompletionCommand completion, PendingSchedule pending) {
		synchronized (this) {
			if (pendingSchedules.get(completion.idempotencyKey()) != pending) {
				return;
			}
			if (!pending.active) {
				pending.fired = true;
				return;
			}
			if (pending.completing) {
				return;
			}
			pending.completing = true;
		}
		complete(completion, pending);
	}

	/** 直接 completion 失败时交给 retry/outbox；未被接管时向 scheduler 暴露异常。 / Sends failed completion to retry/outbox and exposes rejection to the scheduler. */
	private void complete(CompletionCommand completion, PendingSchedule pending) {
		ActionResult direct;
		try {
			direct = Objects.requireNonNull(completer.apply(completion), "item-use completion result");
		} catch (RuntimeException e) {
			direct = ActionResult.FAILED;
		}
		if (accepted(direct)) {
			markCompleted(completion, pending);
			return;
		}
		try {
			ActionResult deferred = Objects.requireNonNull(completionRetry.apply(completion), "item-use completion retry result");
			if (accepted(deferred)) {
				markCompleted(completion, pending);
				return;
			}
		} catch (RuntimeException ignored) {
			// Fall through so the scheduler can observe that neither endpoint accepted ownership.
		}
		synchronized (this) {
			pending.completing = false;
		}
		throw new IllegalStateException("Item-use completion was not accepted: " + completion.idempotencyKey());
	}

	private synchronized void markCompleted(CompletionCommand completion, PendingSchedule pending) {
		pendingSchedules.remove(completion.idempotencyKey(), pending);
		pending.completing = false;
	}

	private static boolean accepted(ActionResult result) {
		return result == ActionResult.APPLIED || result == ActionResult.ALREADY_APPLIED;
	}

	private static boolean cancel(CancellationHandle cancellation) {
		if (cancellation == null) {
			return true;
		}
		try {
			cancellation.cancel();
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static final class PendingSchedule {
		private CancellationHandle cancellation;
		private boolean active;
		private boolean fired;
		private boolean completing;
	}

	/** 广播正式物品使用开始动画。 / Broadcasts the production item-use start animation. */
	private static ActionResult startAnimation(Player player, StartCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		PacketSendUtility.broadcastPacket(player,
			new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), command.itemObjectId(), command.itemId(), command.durationMs(), 0, 0), true);
		return ActionResult.APPLIED;
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}

	/** 表示动画启动命令。 / Represents the animation-start command. */
	public record StartCommand(int questId, int playerId, int itemId, int itemObjectId, int durationMs, String idempotencyKey) {
		/** 校验启动命令字段。 / Validates start-command fields. */
		public StartCommand {
			if (questId <= 0 || playerId <= 0 || itemId <= 0 || itemObjectId <= 0 || durationMs <= 0
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Item-use animation start command is invalid");
			}
		}
	}

	/** 表示延迟完成后的结束动画与对话投影。 / Represents end-animation plus dialog projection after the delay. */
	public record CompletionCommand(int questId, int playerId, int itemId, int itemObjectId, int dialogId, String idempotencyKey) {
		/** 校验完成命令字段。 / Validates completion-command fields. */
		public CompletionCommand {
			if (questId <= 0 || playerId <= 0 || itemId <= 0 || itemObjectId <= 0 || dialogId <= 0
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Item-use animation completion command is invalid");
			}
		}
	}

	/** 可由会话清理真正取消的 scheduler 句柄。 / Scheduler handle that session cleanup can actually cancel. */
	@FunctionalInterface
	public interface CancellationHandle {
		void cancel();
	}

	/** 表示 scheduler 是否接管及其取消句柄。 / Represents scheduler acceptance and its cancellation handle. */
	public record ScheduleResult(ActionResult result, CancellationHandle cancellation) {
		/** 接管成功必须返回可取消句柄。 / Accepted scheduling requires a cancellation handle. */
		public ScheduleResult {
			Objects.requireNonNull(result, "schedule result");
			if (accepted(result) && cancellation == null) {
				throw new IllegalArgumentException("Accepted schedule requires a cancellation handle");
			}
		}
	}
}
