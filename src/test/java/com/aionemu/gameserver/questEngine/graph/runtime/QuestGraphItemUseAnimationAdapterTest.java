package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DelayItemUseContinuationAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ScheduleItemUseDialogAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.CompletionCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ContinuationCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ScheduleResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.StartCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

/**
 * 验证物品使用动画延迟协议的幂等、事件绑定与会话清理。
 * Verifies idempotency, event binding, and session cleanup for the item-use animation delay protocol.
 */
class QuestGraphItemUseAnimationAdapterTest {

	/**
	 * 验证启动动画后调度完成，并占用稳定幂等键防止重放。
	 * Verifies animation start then scheduled completion, claiming the stable key against replay.
	 */
	@Test
	void schedulesCompletionAfterAnimationStartIdempotently() {
		AtomicInteger starts = new AtomicInteger();
		AtomicInteger schedules = new AtomicInteger();
		List<Runnable> pending = new ArrayList<>();
		AtomicReference<StartCommand> start = new AtomicReference<>();
		AtomicReference<CompletionCommand> completion = new AtomicReference<>();
		QuestGraphItemUseAnimationAdapter adapter = new QuestGraphItemUseAnimationAdapter(7, command -> {
			start.set(command);
			starts.incrementAndGet();
			return ActionResult.APPLIED;
		}, (command, finish) -> {
			schedules.incrementAndGet();
			pending.add(finish);
			return new ScheduleResult(ActionResult.APPLIED, () -> pending.remove(finish));
		}, command -> {
			completion.set(command);
			return ActionResult.APPLIED;
		});
		ActionInvocation invocation = itemUse(7, new ScheduleItemUseDialogAction(3000, 4), "item-use-key");

		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(invocation));
		assertEquals(1, starts.get());
		assertEquals(1, schedules.get());
		assertEquals(new StartCommand(1, 7, 182206034, 55, 3000, "item-use-key"), start.get());
		assertEquals(1, adapter.size());
		assertEquals(1, pending.size());

		pending.get(0).run();
		assertEquals(new CompletionCommand(1, 7, 182206034, 55, 4, "item-use-key"), completion.get());
	}

	/**
	 * 验证非 ITEM_USE、错误 owner、未知 action 失败关闭，clear 释放会话键。
	 * Verifies non-ITEM_USE, wrong owner, and unknown action fail closed; clear releases session keys.
	 */
	@Test
	void rejectsWrongEventOwnerOrActionAndClearsSession() {
		AtomicInteger starts = new AtomicInteger();
		AtomicInteger cancellations = new AtomicInteger();
		AtomicInteger completions = new AtomicInteger();
		List<Runnable> pending = new ArrayList<>();
		QuestGraphItemUseAnimationAdapter adapter = new QuestGraphItemUseAnimationAdapter(7, command -> {
			starts.incrementAndGet();
			return ActionResult.APPLIED;
		}, (command, finish) -> {
			pending.add(finish);
			return new ScheduleResult(ActionResult.APPLIED, () -> {
				cancellations.incrementAndGet();
				pending.remove(finish);
			});
		}, command -> {
			completions.incrementAndGet();
			return ActionResult.APPLIED;
		});

		assertEquals(ActionResult.FAILED, adapter.execute(itemUse(8, new ScheduleItemUseDialogAction(3000, 4), "wrong-owner")));
		assertEquals(ActionResult.FAILED, adapter.execute(dialog(7, new ScheduleItemUseDialogAction(3000, 4), "wrong-event")));
		assertEquals(ActionResult.FAILED, adapter.execute(itemUse(7, new PlayMovieAction(913), "wrong-action")));

		ActionInvocation valid = itemUse(7, new ScheduleItemUseDialogAction(3000, 4), "session-key");
		assertEquals(ActionResult.APPLIED, adapter.execute(valid));
		Runnable cancelled = pending.get(0);
		assertEquals(ActionResult.APPLIED, adapter.clear());
		assertEquals(1, cancellations.get());
		assertEquals(0, adapter.size());
		cancelled.run();
		assertEquals(0, completions.get());
		assertEquals(ActionResult.APPLIED, adapter.execute(valid));
		assertEquals(2, starts.get());
	}

	/** 验证同步 scheduler 的 callback 不会在 handle 登记前丢失。 / Verifies a synchronous scheduler callback is not lost before handle registration. */
	@Test
	void synchronousSchedulerCompletesExactlyOnce() {
		AtomicInteger completions = new AtomicInteger();
		QuestGraphItemUseAnimationAdapter adapter = new QuestGraphItemUseAnimationAdapter(7, command -> ActionResult.APPLIED,
			(command, finish) -> {
				finish.run();
				return new ScheduleResult(ActionResult.APPLIED, () -> {
				});
			}, command -> {
				completions.incrementAndGet();
				return ActionResult.APPLIED;
			});

		assertEquals(ActionResult.APPLIED,
			adapter.execute(itemUse(7, new ScheduleItemUseDialogAction(3000, 4), "sync-key")));
		assertEquals(1, completions.get());
	}

	/** 验证取消失败会保留 pending 状态，允许 lifecycle cleanup 重试。 / Verifies cancellation failure retains pending state for lifecycle cleanup retry. */
	@Test
	void failedCancellationRemainsRetryable() {
		AtomicInteger cancellations = new AtomicInteger();
		QuestGraphItemUseAnimationAdapter adapter = new QuestGraphItemUseAnimationAdapter(7, command -> ActionResult.APPLIED,
			(command, finish) -> new ScheduleResult(ActionResult.APPLIED, () -> {
				if (cancellations.incrementAndGet() == 1) {
					throw new IllegalStateException("busy");
				}
			}), command -> ActionResult.APPLIED);
		assertEquals(ActionResult.APPLIED,
			adapter.execute(itemUse(7, new ScheduleItemUseDialogAction(3000, 4), "cancel-retry")));

		assertEquals(ActionResult.FAILED, adapter.clear());
		assertEquals(1, adapter.size());
		assertEquals(ActionResult.APPLIED, adapter.clear());
		assertEquals(0, adapter.size());
		assertEquals(2, cancellations.get());
	}

	/** 验证 completion 失败进入显式 retry，retry 未接管时由 scheduler 观察异常。 / Verifies failed completion uses retry and remains scheduler-observable when unaccepted. */
	@Test
	void completionFailureUsesRetryAndDoesNotDisappear() {
		List<Runnable> pending = new ArrayList<>();
		AtomicReference<CompletionCommand> retried = new AtomicReference<>();
		QuestGraphItemUseAnimationAdapter accepted = new QuestGraphItemUseAnimationAdapter(7, command -> ActionResult.APPLIED,
			(command, finish) -> {
				pending.add(finish);
				return new ScheduleResult(ActionResult.APPLIED, () -> pending.remove(finish));
			}, command -> ActionResult.FAILED, command -> {
				retried.set(command);
				return ActionResult.APPLIED;
			});
		assertEquals(ActionResult.APPLIED,
			accepted.execute(itemUse(7, new ScheduleItemUseDialogAction(3000, 4), "retry-key")));
		pending.remove(0).run();
		assertEquals("retry-key", retried.get().idempotencyKey());

		List<Runnable> rejectedPending = new ArrayList<>();
		QuestGraphItemUseAnimationAdapter rejected = new QuestGraphItemUseAnimationAdapter(7, command -> ActionResult.APPLIED,
			(command, finish) -> {
				rejectedPending.add(finish);
				return new ScheduleResult(ActionResult.APPLIED, () -> rejectedPending.remove(finish));
			}, command -> ActionResult.FAILED, command -> ActionResult.FAILED);
		assertEquals(ActionResult.APPLIED,
			rejected.execute(itemUse(7, new ScheduleItemUseDialogAction(3000, 4), "unaccepted-key")));
		assertThrows(IllegalStateException.class, rejectedPending.remove(0)::run);
	}

	@Test
	void durableContinuationUsesAbsoluteReadyAtAndSchedulesOnlyOnce() {
		AtomicLong now = new AtomicLong(1_700_000_001_250L);
		AtomicInteger starts = new AtomicInteger();
		AtomicInteger schedules = new AtomicInteger();
		AtomicReference<StartCommand> start = new AtomicReference<>();
		AtomicReference<ContinuationCommand> scheduled = new AtomicReference<>();
		AtomicLong delay = new AtomicLong();
		QuestGraphItemUseAnimationAdapter adapter = durableAdapter(now, command -> {
			start.set(command);
			starts.incrementAndGet();
			return ActionResult.APPLIED;
		}, (command, wakeup, delayMillis) -> {
			scheduled.set(command);
			delay.set(delayMillis);
			schedules.incrementAndGet();
			return new ScheduleResult(ActionResult.APPLIED, () -> {
			});
		}, command -> ActionResult.APPLIED, command -> ActionResult.APPLIED);
		ActionInvocation invocation = durableItemUse("durable-key");

		assertEquals(ActionResult.DEFERRED, adapter.execute(invocation));
		assertEquals(ActionResult.DEFERRED, adapter.execute(invocation));
		assertEquals(1, starts.get());
		assertEquals(1, schedules.get());
		assertEquals(1750, delay.get());
		assertEquals(new StartCommand(1, 7, 182206034, 55, 1750, "durable-key"), start.get());
		assertEquals(1_700_000_003_000L, scheduled.get().readyAt());
		assertEquals(-1, scheduled.get().baseRevision());
		assertEquals("item-use", scheduled.get().transitionId());
	}

	@Test
	void durableSynchronousSchedulerWakeupIsNotLost() {
		AtomicInteger resumptions = new AtomicInteger();
		QuestGraphItemUseAnimationAdapter adapter = durableAdapter(new AtomicLong(1_700_000_001_000L),
			command -> ActionResult.APPLIED, (command, wakeup, delayMillis) -> {
				wakeup.run();
				return new ScheduleResult(ActionResult.APPLIED, () -> {
				});
			}, command -> {
				resumptions.incrementAndGet();
				return ActionResult.APPLIED;
			}, command -> ActionResult.APPLIED);

		assertEquals(ActionResult.DEFERRED, adapter.execute(durableItemUse("sync-durable-key")));
		assertEquals(1, resumptions.get());
		assertEquals(0, adapter.size());
	}

	@Test
	void durableContinuationAtDeadlineEndsAnimationWithoutScheduling() {
		AtomicInteger schedules = new AtomicInteger();
		AtomicInteger resumptions = new AtomicInteger();
		AtomicReference<ContinuationCommand> ended = new AtomicReference<>();
		QuestGraphItemUseAnimationAdapter adapter = durableAdapter(new AtomicLong(1_700_000_003_000L),
			command -> ActionResult.APPLIED, (command, wakeup, delayMillis) -> {
				schedules.incrementAndGet();
				return new ScheduleResult(ActionResult.APPLIED, () -> {
				});
			}, command -> {
				resumptions.incrementAndGet();
				return ActionResult.APPLIED;
			}, command -> {
				ended.set(command);
				return ActionResult.APPLIED;
			});
		ActionInvocation invocation = durableItemUse("expired-key");

		assertEquals(ActionResult.APPLIED, adapter.execute(invocation));
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(invocation));
		assertEquals(0, schedules.get());
		assertEquals(0, resumptions.get());
		assertEquals(1_700_000_003_000L, ended.get().readyAt());
	}

	@Test
	void durableClearCancelsScheduleAndIgnoresStaleWakeup() {
		AtomicInteger cancellations = new AtomicInteger();
		AtomicInteger resumptions = new AtomicInteger();
		AtomicReference<Runnable> wakeup = new AtomicReference<>();
		QuestGraphItemUseAnimationAdapter adapter = durableAdapter(new AtomicLong(1_700_000_001_000L),
			command -> ActionResult.APPLIED, (command, callback, delayMillis) -> {
				wakeup.set(callback);
				return new ScheduleResult(ActionResult.APPLIED, cancellations::incrementAndGet);
			}, command -> {
				resumptions.incrementAndGet();
				return ActionResult.APPLIED;
			}, command -> ActionResult.APPLIED);

		assertEquals(ActionResult.DEFERRED, adapter.execute(durableItemUse("clear-key")));
		assertEquals(ActionResult.APPLIED, adapter.clear());
		assertEquals(1, cancellations.get());
		assertEquals(0, adapter.size());
		wakeup.get().run();
		assertEquals(0, resumptions.get());
	}

	private static QuestGraphItemUseAnimationAdapter durableAdapter(AtomicLong now,
			java.util.function.Function<StartCommand, ActionResult> starter,
			QuestGraphItemUseAnimationAdapter.ContinuationScheduler scheduler,
			java.util.function.Function<ContinuationCommand, ActionResult> resumer,
			java.util.function.Function<ContinuationCommand, ActionResult> ender) {
		return new QuestGraphItemUseAnimationAdapter(7, starter,
			(command, finish) -> new ScheduleResult(ActionResult.FAILED, null), command -> ActionResult.FAILED,
			scheduler, resumer, ender, now::get);
	}

	private static ActionInvocation durableItemUse(String key) {
		return new ActionInvocation(new DelayItemUseContinuationAction(3000), 1, 0, QuestStatus.NONE,
			new ItemUseEvent("item-use-event", 7, 1_700_000_000_000L, 182206034, 55),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key).withJournalIdentity(-1, "item-use");
	}

	private static ActionInvocation itemUse(int playerId,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action, String key) {
		return new ActionInvocation(action, 1, 0, QuestStatus.NONE,
			new ItemUseEvent("item-use", playerId, 1_700_000_000_000L, 182206034, 55),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}

	private static ActionInvocation dialog(int playerId,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action, String key) {
		return new ActionInvocation(action, 1, 0, QuestStatus.NONE,
			new DialogEvent("dialog", playerId, 1_700_000_000_000L, 100, "QUEST_SELECT"),
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}
}
