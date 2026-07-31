package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ScheduleItemUseDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DelayItemUseContinuationAction;
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
	private final ContinuationScheduler continuationScheduler;
	private final Function<ContinuationCommand, ActionResult> continuationResumer;
	private final Function<ContinuationCommand, ActionResult> animationEnder;
	private final LongSupplier clock;
	private final Set<String> continuationCompletedKeys = new HashSet<>();
	private final Map<String, PendingSchedule> pendingContinuations = new HashMap<>();

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
		this.continuationScheduler = null;
		this.continuationResumer = null;
		this.animationEnder = null;
		this.clock = null;
	}

	/** 创建正式 durable continuation adapter；唤醒只恢复 journal，不直接执行 tail。 / Creates the production durable continuation adapter; wake-up resumes the journal rather than executing the tail directly. */
	public QuestGraphItemUseAnimationAdapter(Player player, Function<ContinuationCommand, ActionResult> continuationResumer) {
		this(requirePlayer(player).getObjectId(), command -> startAnimation(player, command),
			(command, finish) -> new ScheduleResult(ActionResult.FAILED, null), command -> ActionResult.FAILED,
			(command, finish, delayMillis) -> {
				var future = GameThreadPoolServices.threadPoolManager().schedule(finish, delayMillis);
				return new ScheduleResult(ActionResult.APPLIED, () -> future.cancel(false));
			}, continuationResumer, command -> endAnimation(player, command), System::currentTimeMillis);
	}

	/** 创建可注入时钟与 scheduler 的 durable continuation adapter。 / Creates a durable continuation adapter with injected clock and scheduler. */
	QuestGraphItemUseAnimationAdapter(int playerId, Function<StartCommand, ActionResult> starter,
			BiFunction<CompletionCommand, Runnable, ScheduleResult> scheduler, Function<CompletionCommand, ActionResult> completer,
			ContinuationScheduler continuationScheduler, Function<ContinuationCommand, ActionResult> continuationResumer,
			Function<ContinuationCommand, ActionResult> animationEnder, LongSupplier clock) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Item-use animation adapter player id is invalid");
		}
		this.playerId = playerId;
		this.starter = Objects.requireNonNull(starter, "starter");
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
		this.completer = Objects.requireNonNull(completer, "completer");
		this.completionRetry = command -> ActionResult.FAILED;
		this.continuationScheduler = Objects.requireNonNull(continuationScheduler, "continuationScheduler");
		this.continuationResumer = Objects.requireNonNull(continuationResumer, "continuationResumer");
		this.animationEnder = Objects.requireNonNull(animationEnder, "animationEnder");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	/**
	 * 启动动画并调度延迟对话；稳定幂等键已接受时不重复调度，错误事件/owner 显式失败。
	 * Starts animation and schedules the delayed dialog; an accepted stable key is not rescheduled, wrong event/owner fails closed.
	 */
	public synchronized ActionResult execute(ActionInvocation invocation) {
		if (invocation != null && invocation.action() instanceof DelayItemUseContinuationAction) {
			return executeContinuation(invocation);
		}
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

	/** 只读验证 delay barrier 的 owner、事件和稳定 tail 身份。 / Read-only validates a delay barrier's owner, event, and stable tail identity. */
	public synchronized com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult preflight(
			ActionInvocation invocation) {
		return continuationScheduler != null && validContinuationInvocation(invocation, false)
			? com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.READY
			: com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.FAILED;
	}

	private ActionResult executeContinuation(ActionInvocation invocation) {
		if (continuationScheduler == null || !validContinuationInvocation(invocation, true)) {
			return ActionResult.FAILED;
		}
		DelayItemUseContinuationAction action = (DelayItemUseContinuationAction) invocation.action();
		ItemUseEvent event = (ItemUseEvent) invocation.event();
		long readyAt;
		long now;
		try {
			readyAt = Math.addExact(event.occurredAt(), action.durationMs());
			now = now();
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		ContinuationCommand command = new ContinuationCommand(invocation.questId(), playerId, invocation.baseRevision(), invocation.transitionId(),
			invocation.actionIndex(), event.eventId(), event.itemId(), event.itemObjectId(), action.durationMs(), readyAt, invocation.idempotencyKey());
		if (continuationCompletedKeys.contains(command.idempotencyKey())) {
			return ActionResult.ALREADY_APPLIED;
		}
		if (now >= readyAt) {
			ActionResult ended;
			try {
				ended = Objects.requireNonNull(animationEnder.apply(command), "item-use animation end result");
			} catch (RuntimeException e) {
				return ActionResult.FAILED;
			}
			if (!accepted(ended)) {
				return ActionResult.FAILED;
			}
			PendingSchedule pending = pendingContinuations.remove(command.idempotencyKey());
			if (pending != null) {
				cancel(pending.cancellation);
			}
			continuationCompletedKeys.add(command.idempotencyKey());
			return ended;
		}
		if (pendingContinuations.containsKey(command.idempotencyKey())) {
			return ActionResult.DEFERRED;
		}
		int remaining = Math.toIntExact(Math.min(Integer.MAX_VALUE, readyAt - now));
		ActionResult started;
		try {
			started = Objects.requireNonNull(starter.apply(new StartCommand(command.questId(), playerId, command.itemId(),
				command.itemObjectId(), remaining, command.idempotencyKey())), "item-use animation start result");
		} catch (RuntimeException e) {
			return ActionResult.FAILED;
		}
		if (!accepted(started)) {
			return ActionResult.FAILED;
		}
		PendingSchedule pending = new PendingSchedule();
		pendingContinuations.put(command.idempotencyKey(), pending);
		ScheduleResult scheduled;
		try {
			scheduled = Objects.requireNonNull(continuationScheduler.schedule(command,
				() -> resumeContinuation(command, pending), readyAt - now), "item-use continuation schedule result");
		} catch (RuntimeException e) {
			pendingContinuations.remove(command.idempotencyKey(), pending);
			return ActionResult.FAILED;
		}
		if (!accepted(scheduled.result())) {
			pendingContinuations.remove(command.idempotencyKey(), pending);
			cancel(scheduled.cancellation());
			return ActionResult.FAILED;
		}
		pending.cancellation = scheduled.cancellation();
		pending.active = true;
		if (pending.fired) {
			resumeContinuation(command, pending);
		}
		return ActionResult.DEFERRED;
	}

	private void resumeContinuation(ContinuationCommand command, PendingSchedule pending) {
		synchronized (this) {
			if (pendingContinuations.get(command.idempotencyKey()) != pending || pending.completing) {
				return;
			}
			if (!pending.active) {
				pending.fired = true;
				return;
			}
			pending.completing = true;
		}
		ActionResult resumed;
		try {
			resumed = Objects.requireNonNull(continuationResumer.apply(command), "item-use continuation resume result");
		} catch (RuntimeException e) {
			resumed = ActionResult.FAILED;
		}
		synchronized (this) {
			if (accepted(resumed)) {
				pendingContinuations.remove(command.idempotencyKey(), pending);
			} else {
				pending.completing = false;
			}
		}
		if (!accepted(resumed)) {
			throw new IllegalStateException("Item-use continuation journal was not resumed: " + command.idempotencyKey());
		}
	}

	private boolean validContinuationInvocation(ActionInvocation invocation, boolean requireJournalIdentity) {
		return invocation != null && invocation.questId() > 0 && invocation.actionIndex() >= 0
			&& invocation.event().playerId() == playerId && invocation.action() instanceof DelayItemUseContinuationAction
			&& invocation.event() instanceof ItemUseEvent && (!requireJournalIdentity || invocation.hasJournalIdentity())
			&& invocation.idempotencyKey() != null && !invocation.idempotencyKey().isBlank();
	}

	private long now() {
		long value = clock.getAsLong();
		if (value <= 0) {
			throw new IllegalStateException("Item-use continuation clock is invalid");
		}
		return value;
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
		for (Iterator<Map.Entry<String, PendingSchedule>> iterator = pendingContinuations.entrySet().iterator(); iterator.hasNext();) {
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
		continuationCompletedKeys.clear();
		return result;
	}

	/** 返回已接受键数量，仅用于确定性测试与审计。 / Returns the accepted-key count only for deterministic tests and audit. */
	public synchronized int size() {
		return acceptedKeys.size() + pendingContinuations.size() + continuationCompletedKeys.size();
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

	/** 广播正式物品使用结束动画。 / Broadcasts the production item-use completion animation. */
	private static ActionResult endAnimation(Player player, ContinuationCommand command) {
		if (player.getObjectId() != command.playerId() || player.getClientConnection() == null) {
			return ActionResult.FAILED;
		}
		PacketSendUtility.broadcastPacket(player,
			new SM_ITEM_USAGE_ANIMATION(player.getObjectId(), command.itemObjectId(), command.itemId(), 0, 1, 0), true);
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

	/** 持久 journal 延迟屏障的冻结唤醒身份。 / Frozen wake-up identity for a durable journal delay barrier. */
	public record ContinuationCommand(int questId, int playerId, long baseRevision, String transitionId, int actionIndex, String eventId,
			int itemId, int itemObjectId, int durationMs, long readyAt, String idempotencyKey) {
		/** 校验 owner、事件物品、绝对时间和稳定键。 / Validates owner, event item, absolute time, and stable key. */
		public ContinuationCommand {
			if (questId <= 0 || playerId <= 0 || baseRevision < -1 || transitionId == null || transitionId.isBlank()
					|| actionIndex < 0 || eventId == null || eventId.isBlank()
					|| itemId <= 0 || itemObjectId <= 0 || durationMs <= 0 || readyAt <= 0
					|| idempotencyKey == null || idempotencyKey.isBlank()) {
				throw new IllegalArgumentException("Item-use continuation command is invalid");
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

	/** 按绝对到期时间的剩余毫秒调度一次 journal 唤醒。 / Schedules one journal wake-up after the remaining absolute-deadline delay. */
	@FunctionalInterface
	interface ContinuationScheduler {
		ScheduleResult schedule(ContinuationCommand command, Runnable wakeup, long delayMillis);
	}
}
