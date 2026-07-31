package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dao.QuestGraphActionOutboxDAO.OperationConflictException;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord;
import com.aionemu.gameserver.questEngine.graph.state.QuestGraphActionOutboxRecord.Status;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand;
import com.aionemu.gameserver.questEngine.graph.state.TeleportOutboxCommand.InstanceRecoveryMode;

class QuestGraphTeleportOutboxTest {

	@Test
	void acceptsBeforeSchedulingAndCollectsOnlyAfterCompletionAndGraphAck() {
		MemoryStore store = new MemoryStore();
		ArrayDeque<Runnable> tasks = new ArrayDeque<>();
		AtomicReference<Runnable> completion = new AtomicReference<>();
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, tasks::add, (command, authorization, callback) -> {
			completion.set(callback);
			return true;
		}, () -> 100);
		TeleportOutboxCommand command = command("accepted");

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(command));
		assertEquals(List.of("accept", "schedule"), store.eventsWithSchedule(tasks.size()));
		assertEquals(Status.ACCEPTED, store.find(7, command.operationKey()).status());

		tasks.removeFirst().run();
		assertEquals(Status.CLAIMED, store.find(7, command.operationKey()).status());
		assertEquals(ActionResult.ALREADY_APPLIED,
			outbox.acknowledgeGraph(command.questId(), command.baseRevision(), command.transitionId(), command.actionIndex(), command.operationKey()));
		assertEquals(true, store.find(7, command.operationKey()).graphAcked());

		completion.get().run();
		assertNull(store.find(7, command.operationKey()));
	}

	@Test
	void durableAcceptanceSurvivesSchedulerFailure() {
		MemoryStore store = new MemoryStore();
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, task -> {
			throw new IllegalStateException("crash after acceptance");
			}, (command, authorization, completion) -> true, () -> 100);

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(command("schedule-crash")));
		assertEquals(Status.ACCEPTED, store.find(7, "schedule-crash").status());
	}

	@Test
	void staleCompletionCannotCompleteSupersededGeneration() {
		MemoryStore store = new MemoryStore();
		ArrayDeque<Runnable> tasks = new ArrayDeque<>();
		AtomicReference<Runnable> staleCompletion = new AtomicReference<>();
		AtomicReference<BooleanSupplier> staleAuthorization = new AtomicReference<>();
		AtomicLong clock = new AtomicLong(100);
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, tasks::add, (command, authorization, completion) -> {
			staleAuthorization.set(authorization);
			staleCompletion.set(completion);
			return true;
		}, clock::get);
		TeleportOutboxCommand command = command("stale");

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(command));
		tasks.removeFirst().run();
		assertEquals(true, staleAuthorization.get().getAsBoolean());
		QuestGraphActionOutboxRecord generationTwo = store.reclaimForRecovery(7, command.operationKey(), 101, 40_000);
		assertEquals(2, generationTwo.claimGeneration());
		assertEquals(false, staleAuthorization.get().getAsBoolean());

		staleCompletion.get().run();
		QuestGraphActionOutboxRecord current = store.find(7, command.operationKey());
		assertEquals(Status.CLAIMED, current.status());
		assertEquals(2, current.claimGeneration());
	}

	@Test
	void sameMillisecondTeleportsRunInDurableSequenceEvenWhenTailDeliveryRunsFirst() {
		MemoryStore store = new MemoryStore();
		List<ScheduledTask> tasks = new ArrayList<>();
		List<String> starts = new ArrayList<>();
		Map<String, Runnable> completions = new LinkedHashMap<>();
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store,
			(task, delayMillis) -> tasks.add(new ScheduledTask(task, delayMillis)),
			(command, authorization, completion) -> {
				assertEquals(true, authorization.getAsBoolean());
				starts.add(command.operationKey());
				completions.put(command.operationKey(), completion);
				return true;
			}, () -> 100);
		TeleportOutboxCommand first = command("first");
		TeleportOutboxCommand second = command("second");

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(first));
		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(second));
		assertEquals(100, store.find(7, first.operationKey()).acceptedAt());
		assertEquals(100, store.find(7, second.operationKey()).acceptedAt());
		assertEquals(true, store.find(7, first.operationKey()).outboxSequence()
			< store.find(7, second.operationKey()).outboxSequence());

		tasks.remove(1).task().run();
		assertEquals(List.of(), starts);
		tasks.remove(0).task().run();
		assertEquals(List.of("first"), starts);
		assertEquals(ActionResult.ALREADY_APPLIED,
			outbox.acknowledgeGraph(first.questId(), first.baseRevision(), first.transitionId(), first.actionIndex(), first.operationKey()));
		completions.get("first").run();

		ScheduledTask secondDelivery = tasks.stream().filter(task -> task.delayMillis() == 0).findFirst().orElseThrow();
		secondDelivery.task().run();
		assertEquals(List.of("first", "second"), starts);
	}

	@Test
	void loginRecoveryRestoresSameMillisecondTeleportsInDurableSequence() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand first = command("first");
		TeleportOutboxCommand second = command("second");
		store.acceptExact(first, 100);
		store.acceptExact(second, 100);
		List<String> restorations = new ArrayList<>();
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> activeState(questId, 100), command -> restorations.add(command.operationKey()), () -> 200);

		recovery.recover();

		assertEquals(List.of("first", "second"), restorations);
		assertEquals(List.of(), store.listPendingForPlayer(7));
	}

	@Test
	void loginRecoveryStopsAtCompletedHeadUntilItsGraphCursorAdvances() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand first = command("first-awaiting-graph");
		TeleportOutboxCommand second = command("second-behind-head");
		store.acceptExact(first, 100);
		store.acceptExact(second, 100);
		List<String> restorations = new ArrayList<>();
		AtomicReference<PlayerQuestGraphState> graphState = new AtomicReference<>(activeState(first.questId(), 7));
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> graphState.get(), command -> restorations.add(command.operationKey()), () -> 200);

		recovery.recover();

		assertEquals(List.of(first.operationKey()), restorations);
		assertEquals(Status.COMPLETED, store.find(7, first.operationKey()).status());
		assertEquals(Status.ACCEPTED, store.find(7, second.operationKey()).status());

		graphState.set(activeState(first.questId(), 8));
		recovery.recover();

		assertEquals(List.of(first.operationKey(), second.operationKey()), restorations);
		assertEquals(List.of(), store.listPendingForPlayer(7));
	}

	@Test
	void rejectedOnlineDeliveryRetriesAfterLeaseAndRejectsStaleGenerationCompletion() {
		MemoryStore store = new MemoryStore();
		ArrayDeque<ScheduledTask> tasks = new ArrayDeque<>();
		List<Runnable> completions = new ArrayList<>();
		AtomicInteger starts = new AtomicInteger();
		AtomicLong clock = new AtomicLong(100);
		QuestGraphTeleportOutbox.Scheduler scheduler = (task, delayMillis) -> tasks.add(new ScheduledTask(task, delayMillis));
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, scheduler, (command, authorization, completion) -> {
			completions.add(completion);
			return starts.incrementAndGet() > 1;
		}, clock::get);
		TeleportOutboxCommand command = command("online-retry");

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(command));
		ScheduledTask initial = tasks.removeFirst();
		assertEquals(0, initial.delayMillis());
		initial.task().run();

		QuestGraphActionOutboxRecord firstClaim = store.find(7, command.operationKey());
		assertEquals(1, firstClaim.claimGeneration());
		ScheduledTask retry = tasks.removeFirst();
		assertEquals(QuestGraphTeleportOutbox.CLAIM_LEASE_MILLIS, retry.delayMillis());

		clock.set(firstClaim.leaseUntil());
		retry.task().run();
		QuestGraphActionOutboxRecord secondClaim = store.find(7, command.operationKey());
		assertEquals(2, secondClaim.claimGeneration());
		assertEquals(2, starts.get());

		completions.get(0).run();
		assertEquals(Status.CLAIMED, store.find(7, command.operationKey()).status());
		assertEquals(2, store.find(7, command.operationKey()).claimGeneration());

		completions.get(1).run();
		assertEquals(Status.COMPLETED, store.find(7, command.operationKey()).status());
	}

	@Test
	void acceptedStartWithoutCompletionRetriesAfterLeaseAndCompletesNextGeneration() {
		MemoryStore store = new MemoryStore();
		ArrayDeque<ScheduledTask> tasks = new ArrayDeque<>();
		List<Runnable> completions = new ArrayList<>();
		AtomicInteger starts = new AtomicInteger();
		AtomicLong clock = new AtomicLong(100);
		QuestGraphTeleportOutbox.Scheduler scheduler = (task, delayMillis) -> tasks.add(new ScheduledTask(task, delayMillis));
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, scheduler, (command, authorization, completion) -> {
			starts.incrementAndGet();
			completions.add(completion);
			return true;
		}, clock::get);
		TeleportOutboxCommand command = command("missing-completion");

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(command));
		tasks.removeFirst().task().run();
		QuestGraphActionOutboxRecord firstClaim = store.find(7, command.operationKey());
		assertEquals(1, firstClaim.claimGeneration());
		assertEquals(1, starts.get());

		ScheduledTask firstWatchdog = tasks.removeFirst();
		assertEquals(QuestGraphTeleportOutbox.CLAIM_LEASE_MILLIS, firstWatchdog.delayMillis());
		clock.set(firstClaim.leaseUntil());
		firstWatchdog.task().run();
		QuestGraphActionOutboxRecord secondClaim = store.find(7, command.operationKey());
		assertEquals(2, secondClaim.claimGeneration());
		assertEquals(2, starts.get());

		completions.get(1).run();
		assertEquals(Status.COMPLETED, store.find(7, command.operationKey()).status());
	}

	@Test
	void completedGenerationMakesItsLeaseWatchdogANoOp() {
		MemoryStore store = new MemoryStore();
		ArrayDeque<ScheduledTask> tasks = new ArrayDeque<>();
		AtomicReference<Runnable> completion = new AtomicReference<>();
		AtomicInteger starts = new AtomicInteger();
		AtomicLong clock = new AtomicLong(100);
		QuestGraphTeleportOutbox.Scheduler scheduler = (task, delayMillis) -> tasks.add(new ScheduledTask(task, delayMillis));
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, scheduler, (command, authorization, callback) -> {
			starts.incrementAndGet();
			completion.set(callback);
			return true;
		}, clock::get);
		TeleportOutboxCommand command = command("completed-watchdog");

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(command));
		tasks.removeFirst().task().run();
		QuestGraphActionOutboxRecord claimed = store.find(7, command.operationKey());
		ScheduledTask watchdog = tasks.removeFirst();
		completion.get().run();
		assertEquals(Status.COMPLETED, store.find(7, command.operationKey()).status());

		clock.set(claimed.leaseUntil());
		watchdog.task().run();
		assertEquals(Status.COMPLETED, store.find(7, command.operationKey()).status());
		assertEquals(1, starts.get());
		assertEquals(0, tasks.size());
		assertEquals(ActionResult.ALREADY_APPLIED,
			outbox.acknowledgeGraph(command.questId(), command.baseRevision(), command.transitionId(), command.actionIndex(), command.operationKey()));
		assertNull(store.find(7, command.operationKey()));
	}

	@Test
	void sameKeyWithDifferentPayloadFailsClosed() {
		MemoryStore store = new MemoryStore();
		QuestGraphTeleportOutbox outbox = new QuestGraphTeleportOutbox(7, store, task -> { }, (command, authorization, completion) -> true, () -> 100);
		TeleportOutboxCommand original = command("conflict");
		TeleportOutboxCommand changed = new TeleportOutboxCommand(7, original.questId(), original.baseRevision(), original.transitionId(),
			original.actionIndex(), 220010000, 1, original.instanceRecoveryMode(), original.x(), original.y(), original.z(), original.heading(),
			original.operationKey());

		assertEquals(ActionResult.DURABLY_ACCEPTED, outbox.accept(original));
		assertEquals(ActionResult.FAILED, outbox.accept(changed));
	}

	@Test
	void loginRecoverySupersedesOldLeaseRestoresBeforeSpawnAndReconcilesAck() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand command = command("login-recovery");
		store.acceptExact(command, 100);
		QuestGraphActionOutboxRecord abandoned = store.claim(7, command.operationKey(), 100, 50_000);
		List<String> restored = new ArrayList<>();
		PlayerQuestGraphState advanced = activeState(command.questId(), 8);
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> advanced, accepted -> restored.add(accepted.operationKey()), () -> 200);

		recovery.recover();

		assertEquals(1, abandoned.claimGeneration());
		assertEquals(List.of(command.operationKey()), restored);
		assertNull(store.find(7, command.operationKey()));
		assertEquals(2, store.lastCompletedGeneration);
	}

	@Test
	void loginRecoveryKeepsCompletedRowUntilGraphRevisionProvesTheActionCursor() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand command = command("await-graph");
		store.acceptExact(command, 100);
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> activeState(command.questId(), 7), accepted -> { }, () -> 200);

		recovery.recover();

		QuestGraphActionOutboxRecord retained = store.find(7, command.operationKey());
		assertEquals(Status.COMPLETED, retained.status());
		assertEquals(false, retained.graphAcked());
	}

	@Test
	void loginRecoveryAcknowledgesAnAlreadyCompletedCommandWithoutRestoringPositionAgain() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand command = command("completed-before-login");
		store.acceptExact(command, 100);
		QuestGraphActionOutboxRecord claimed = store.claim(7, command.operationKey(), 100, 50_000);
		store.complete(7, command.operationKey(), claimed.claimGeneration(), 200);
		AtomicInteger restorations = new AtomicInteger();
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> activeState(command.questId(), 8), accepted -> restorations.incrementAndGet(), () -> 300);

		recovery.recover();

		assertEquals(0, restorations.get());
		assertNull(store.find(7, command.operationKey()));
	}

	@Test
	void loginRecoveryGarbageCollectsATerminalCommandWithoutRestoringPositionAgain() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand command = command("terminal-before-login");
		store.acceptExact(command, 100);
		QuestGraphActionOutboxRecord claimed = store.claim(7, command.operationKey(), 100, 50_000);
		store.ackGraph(7, command.operationKey());
		store.complete(7, command.operationKey(), claimed.claimGeneration(), 200);
		AtomicInteger restorations = new AtomicInteger();
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> activeState(command.questId(), 8), accepted -> restorations.incrementAndGet(), () -> 300);

		recovery.recover();

		assertEquals(0, restorations.get());
		assertNull(store.find(7, command.operationKey()));
	}

	@Test
	void loginRecoveryToleratesAnOldGenerationCompletingAndDeletingBeforeReclaim() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand command = command("terminal-race-before-reclaim");
		store.acceptExact(command, 100);
		QuestGraphActionOutboxRecord oldClaim = store.claim(7, command.operationKey(), 100, 50_000);
		store.ackGraph(7, command.operationKey());
		store.beforeRecoveryClaim = () -> {
			store.complete(7, command.operationKey(), oldClaim.claimGeneration(), 200);
			store.deleteAcked(7, command.operationKey());
		};
		AtomicInteger restorations = new AtomicInteger();
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> activeState(command.questId(), 8), accepted -> restorations.incrementAndGet(), () -> 300);

		recovery.recover();

		assertEquals(0, restorations.get());
		assertNull(store.find(7, command.operationKey()));
	}

	@Test
	void loginRecoveryReclaimsBeforeRestoringSoAnOldCallbackCannotCompleteOrDelete() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand command = command("stale-callback-during-recovery");
		store.acceptExact(command, 100);
		QuestGraphActionOutboxRecord oldClaim = store.claim(7, command.operationKey(), 100, 50_000);
		store.ackGraph(7, command.operationKey());
		AtomicInteger restorations = new AtomicInteger();
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> activeState(command.questId(), 8), accepted -> {
				restorations.incrementAndGet();
				assertEquals(false, store.complete(7, command.operationKey(), oldClaim.claimGeneration(), 200));
				assertEquals(false, store.deleteAcked(7, command.operationKey()));
			}, () -> 300);

		recovery.recover();

		assertEquals(1, restorations.get());
		assertEquals(2, store.lastCompletedGeneration);
		assertNull(store.find(7, command.operationKey()));
	}

	@Test
	void quarantinedRevisionCannotFalselyAcknowledgeTheTeleportAction() {
		MemoryStore store = new MemoryStore();
		TeleportOutboxCommand command = command("quarantined");
		store.acceptExact(command, 100);
		PlayerQuestGraphState quarantined = new PlayerQuestGraphState(command.questId(), 1, 8, "active", QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.QUARANTINED, Map.of(), Map.of(), null, Map.of(), "test quarantine");
		QuestGraphTeleportRecoveryCoordinator recovery = new QuestGraphTeleportRecoveryCoordinator(7, store,
			questId -> quarantined, accepted -> { }, () -> 200);

		recovery.recover();

		QuestGraphActionOutboxRecord retained = store.find(7, command.operationKey());
		assertEquals(Status.COMPLETED, retained.status());
		assertEquals(false, retained.graphAcked());
	}

	@Test
	void explicitlyFallbackCapableRecoveryModesHandleARejectedFrozenInstance() {
		for (InstanceRecoveryMode mode : List.of(
				InstanceRecoveryMode.PLAYER_REGISTERED_OR_CREATE, InstanceRecoveryMode.DEFAULT_INSTANCE)) {
			AtomicInteger fallbacks = new AtomicInteger();
			String recovered = QuestGraphTeleportRecoveryCoordinator.resolveRecoveryInstance(mode,
				() -> { throw new IllegalArgumentException("instance exceeds twin count"); },
				() -> { fallbacks.incrementAndGet(); return mode.name(); });

			assertEquals(mode.name(), recovered);
			assertEquals(1, fallbacks.get());
		}
	}

	@Test
	void frozenRecoveryModesFailClosedWhenTheirInstanceIsRejectedOrMissing() {
		AtomicInteger fallbacks = new AtomicInteger();

		for (InstanceRecoveryMode mode : List.of(InstanceRecoveryMode.EXACT, InstanceRecoveryMode.PLAYER_CURRENT)) {
			assertThrows(IllegalStateException.class,
				() -> QuestGraphTeleportRecoveryCoordinator.resolveRecoveryInstance(mode,
					() -> { throw new IllegalArgumentException("instance exceeds twin count"); },
					() -> { fallbacks.incrementAndGet(); return "unsafe"; }));
			assertThrows(IllegalStateException.class,
				() -> QuestGraphTeleportRecoveryCoordinator.resolveRecoveryInstance(mode,
					() -> null, () -> { fallbacks.incrementAndGet(); return "unsafe"; }));
		}
		assertEquals(0, fallbacks.get());
	}

	private static TeleportOutboxCommand command(String key) {
		return new TeleportOutboxCommand(7, 2634, 4, "accept", 2, 210010000, 1,
			TeleportOutboxCommand.InstanceRecoveryMode.EXACT, 1, 2, 3, (byte) 4, key);
	}

	private static PlayerQuestGraphState activeState(int questId, long revision) {
		return new PlayerQuestGraphState(questId, 1, revision, "active", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
	}

	private record ScheduledTask(Runnable task, long delayMillis) {
	}

	static final class MemoryStore implements QuestGraphTeleportOutbox.Store {
		private final Map<String, QuestGraphActionOutboxRecord> records = new LinkedHashMap<>();
		private final List<String> events = new ArrayList<>();
		private long lastCompletedGeneration;
		private long nextSequence = 1;
		private Runnable beforeRecoveryClaim;

		@Override
		public QuestGraphActionOutboxRecord acceptExact(TeleportOutboxCommand command, long acceptedAt) {
			QuestGraphActionOutboxRecord existing = records.get(command.operationKey());
			if (existing != null) {
				if (!existing.command().equals(command)) {
					throw new OperationConflictException(command.operationKey());
				}
				return existing;
			}
			events.add("accept");
			QuestGraphActionOutboxRecord accepted = QuestGraphActionOutboxRecord.accepted(command, nextSequence++, acceptedAt);
			records.put(command.operationKey(), accepted);
			return accepted;
		}

		List<String> eventsWithSchedule(int scheduled) {
			List<String> result = new ArrayList<>(events);
			if (scheduled > 0) {
				result.add("schedule");
			}
			return result;
		}

		@Override
		public QuestGraphActionOutboxRecord find(int playerId, String operationKey) {
			return records.get(operationKey);
		}

		@Override
		public QuestGraphActionOutboxRecord claim(int playerId, String operationKey, long now, long leaseUntil) {
			QuestGraphActionOutboxRecord current = records.get(operationKey);
			if (!isHead(current) || current.completedAt() != null || current.status() == Status.CLAIMED && current.leaseUntil() > now) {
				return null;
			}
			return claim(current, leaseUntil);
		}

		@Override
		public QuestGraphActionOutboxRecord reclaimForRecovery(int playerId, String operationKey, long now, long leaseUntil) {
			if (beforeRecoveryClaim != null) {
				Runnable hook = beforeRecoveryClaim;
				beforeRecoveryClaim = null;
				hook.run();
			}
			QuestGraphActionOutboxRecord current = records.get(operationKey);
			return !isHead(current) || current.completedAt() != null ? null : claim(current, leaseUntil);
		}

		private boolean isHead(QuestGraphActionOutboxRecord candidate) {
			return candidate != null && records.values().stream()
				.min(Comparator.comparingLong(QuestGraphActionOutboxRecord::outboxSequence)).orElseThrow() == candidate;
		}

		private QuestGraphActionOutboxRecord claim(QuestGraphActionOutboxRecord current, long leaseUntil) {
			QuestGraphActionOutboxRecord claimed = new QuestGraphActionOutboxRecord(current.command(), current.outboxSequence(), Status.CLAIMED,
				current.claimGeneration() + 1, leaseUntil, current.acceptedAt(), null, current.graphAcked());
			records.put(current.command().operationKey(), claimed);
			return claimed;
		}

		@Override
		public boolean complete(int playerId, String operationKey, long generation, long completedAt) {
			QuestGraphActionOutboxRecord current = records.get(operationKey);
			if (current == null || current.status() != Status.CLAIMED || current.claimGeneration() != generation
					|| current.leaseUntil() <= completedAt) {
				return current != null && current.claimGeneration() == generation && current.completedAt() != null;
			}
			lastCompletedGeneration = generation;
			Status status = current.graphAcked() ? Status.GRAPH_ACKED : Status.COMPLETED;
			records.put(operationKey, new QuestGraphActionOutboxRecord(current.command(), current.outboxSequence(), status, generation, null,
				current.acceptedAt(), completedAt, current.graphAcked()));
			return true;
		}

		@Override
		public boolean isCurrentClaim(int playerId, String operationKey, long generation, long now) {
			QuestGraphActionOutboxRecord current = records.get(operationKey);
			return current != null && current.status() == Status.CLAIMED && current.claimGeneration() == generation
				&& current.leaseUntil() > now && current.completedAt() == null;
		}

		@Override
		public boolean ackGraph(int playerId, String operationKey) {
			QuestGraphActionOutboxRecord current = records.get(operationKey);
			if (current == null) {
				return false;
			}
			Status status = current.completedAt() == null ? current.status() : Status.GRAPH_ACKED;
			records.put(operationKey, new QuestGraphActionOutboxRecord(current.command(), current.outboxSequence(), status, current.claimGeneration(),
				current.leaseUntil(), current.acceptedAt(), current.completedAt(), true));
			return true;
		}

		@Override
		public List<QuestGraphActionOutboxRecord> listPendingForPlayer(int playerId) {
			return records.values().stream().sorted(Comparator.comparingLong(QuestGraphActionOutboxRecord::outboxSequence)).toList();
		}

		@Override
		public boolean deleteAcked(int playerId, String operationKey) {
			QuestGraphActionOutboxRecord current = records.get(operationKey);
			return current != null && current.deletable() && records.remove(operationKey, current);
		}
	}
}
