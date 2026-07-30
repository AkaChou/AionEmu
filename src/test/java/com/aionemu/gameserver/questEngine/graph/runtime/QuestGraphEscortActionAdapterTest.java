package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.ALREADY_APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.READY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.controllers.PlayerController;
import com.aionemu.gameserver.dao.QuestGraphResourceOperationDAO.ObjectIdReservationConflictException;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortNpcDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortZoneDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.CleanupCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.CleanupReason;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.EscortSessionRegistry;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.FollowerReservation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.PlayerContext;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.StartCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.StartResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortReachedTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.world.World;

class QuestGraphEscortActionAdapterTest {
	private GameWorldBootstrapServices worldBootstrapServices;

	@AfterEach
	void tearDownWorld() {
		if (worldBootstrapServices != null) {
			worldBootstrapServices.destroy();
		}
	}

	/** 验证 `(player, quest)` lease 幂等，跨 quest 与同 quest 不同 key 均显式冲突。 / Verifies player/quest lease idempotency and explicit cross-quest/different-key conflicts. */
	@Test
	void leaseIsQuestScopedIdempotentAndConflictClosed() {
		AtomicReference<StartCommand> started = new AtomicReference<>();
		AtomicReference<CleanupCommand> cleaned = new AtomicReference<>();
		AtomicInteger starts = new AtomicInteger();
		QuestGraphEscortActionAdapter adapter = new QuestGraphEscortActionAdapter(7, command -> READY, command -> {
			starts.incrementAndGet();
			started.set(command);
			return new StartResult(APPLIED, 990001, true, "old-walker");
		}, command -> {
			cleaned.set(command);
			return APPLIED;
		});
		ActionInvocation first = invocation(2333, spawnZoneAction(), "escort-a");

		assertEquals(READY, adapter.preflight(first));
		assertEquals(APPLIED, adapter.execute(first));
		assertEquals(ALREADY_APPLIED, adapter.execute(first));
		assertEquals(FAILED, adapter.execute(invocation(2333, spawnZoneAction(), "escort-b")));
		assertEquals(FAILED, adapter.execute(invocation(2394, spawnZoneAction(), "escort-c")));
		assertEquals(1, starts.get());
		assertEquals(2333, started.get().questId());
		assertEquals(1, adapter.size());

		assertEquals(APPLIED, adapter.onAbandon(2333));
		assertEquals(CleanupReason.ABANDON, cleaned.get().reason());
		assertEquals(2333, cleaned.get().questId());
		assertEquals(990001, cleaned.get().followerObjectId());
		assertEquals("old-walker", cleaned.get().previousWalkerId());
		assertEquals(0, adapter.size());
		assertEquals(APPLIED, adapter.execute(invocation(2394, spawnZoneAction(), "escort-c")));
	}

	/** 验证清理失败保留 lease，logout/death/abandon 使用明确原因且可以重试。 / Verifies cleanup failure retains the lease and explicit lifecycle reasons can retry it. */
	@Test
	void lifecycleCleanupRetainsFailedLeaseForRetry() {
		AtomicInteger cleanups = new AtomicInteger();
		AtomicReference<CleanupReason> reason = new AtomicReference<>();
		QuestGraphEscortActionAdapter adapter = new QuestGraphEscortActionAdapter(7, command -> READY,
			command -> new StartResult(APPLIED, 990002, false, "4212"), command -> {
				reason.set(command.reason());
				return cleanups.incrementAndGet() == 1 ? FAILED : APPLIED;
			});
		assertEquals(APPLIED, adapter.execute(invocation(3212, eventCoordinatesAction(), "escort")));

		assertEquals(FAILED, adapter.onPlayerDeath());
		assertEquals(CleanupReason.PLAYER_DEATH, reason.get());
		assertEquals(1, adapter.size());
		assertEquals(APPLIED, adapter.onLogout());
		assertEquals(CleanupReason.LOGOUT, reason.get());
		assertEquals(0, adapter.size());
		assertEquals(ALREADY_APPLIED, adapter.onAbandon(3212));
	}

	/** 验证启动失败不创建 lease，EVENT/REPLACE 来源必须绑定不可变 dialog objectId。 / Verifies failed starts create no lease and EVENT/REPLACE sources require immutable dialog object identity. */
	@Test
	void failedStartAndMissingEventObjectIdentityFailClosed() {
		AtomicInteger starts = new AtomicInteger();
		QuestGraphEscortActionAdapter adapter = new QuestGraphEscortActionAdapter(7, command -> READY, command -> {
			starts.incrementAndGet();
			return StartResult.failed();
		}, command -> APPLIED);

		assertEquals(FAILED, adapter.execute(invocation(2634, eventCoordinatesAction(), "missing-object", false)));
		assertEquals(0, starts.get());
		assertEquals(FAILED, adapter.execute(invocation(2634, replaceNpcAction(), "replace")));
		assertEquals(1, starts.get());
		assertEquals(0, adapter.size());
	}

	@Test
	void typedLeaseRehydratesAndCleansAfterAdapterRecreation() {
		AtomicInteger starts = new AtomicInteger();
		ActionInvocation invocation = invocation(2333, spawnZoneAction(), "durable-escort");
		QuestGraphEscortActionAdapter first = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY, command -> {
				starts.incrementAndGet();
				return new StartResult(APPLIED, 990777, true, "old-walker");
			}, command -> APPLIED);
		CleanupLease plan = first.prepareLease(invocation);
		AtomicReference<StartCommand> recoveredPreflight = new AtomicReference<>();
		QuestGraphEscortActionAdapter plannedRecovery = new QuestGraphEscortActionAdapter(7,
			() -> {
				throw new AssertionError("persisted escort plan must replace the current player context");
			}, command -> {
				recoveredPreflight.set(command);
				return READY;
			}, command -> StartResult.failed(), command -> FAILED);
		assertEquals(READY, plannedRecovery.preflight(withLease(invocation, plan)));
		assertEquals(210040000, recoveredPreflight.get().worldId());
		assertEquals(3, recoveredPreflight.get().instanceId());
		assertEquals(10, recoveredPreflight.get().x());
		assertEquals(20, recoveredPreflight.get().y());
		assertEquals(30, recoveredPreflight.get().z());

		assertEquals(APPLIED, first.execute(withLease(invocation, plan)));
		CleanupLease materialized = first.leaseFor(invocation);
		EscortResourceIdentity identity = (EscortResourceIdentity) materialized.identity();
		assertEquals(990777, identity.objectId());
		assertEquals(204416, identity.npcId());
		assertEquals(210040000, identity.worldId());
		assertEquals("old-walker", identity.previousWalkerId());

		AtomicReference<CleanupCommand> cleanup = new AtomicReference<>();
		QuestGraphEscortActionAdapter recovered = new QuestGraphEscortActionAdapter(7, command -> {
			throw new AssertionError("preflight endpoint must not run during rehydrate");
		}, command -> {
			throw new AssertionError("start endpoint must not run for a materialized lease");
		}, command -> {
			cleanup.set(command);
			return APPLIED;
		});
		assertEquals(ALREADY_APPLIED, recovered.execute(withLease(invocation, materialized)));
		assertEquals(APPLIED, recovered.clear(materialized, CleanupReason.LOGOUT));
		assertEquals(990777, cleanup.get().followerObjectId());
		assertEquals(204416, cleanup.get().followerNpcId());
		assertEquals(210040000, cleanup.get().worldId());
		assertEquals(3, cleanup.get().instanceId());
		assertEquals(1, starts.get());
		assertEquals(FAILED, recovered.clear(new CleanupLease("QUEST_ESCORT", "legacy"), CleanupReason.LOGOUT));
	}

	@Test
	void terminalSignalRequiresExactFollowerAndReleasedPersistentLease() {
		AtomicBoolean released = new AtomicBoolean();
		AtomicInteger dispatches = new AtomicInteger();
		AtomicReference<CleanupCommand> cleanup = new AtomicReference<>();
		QuestGraphEscortActionAdapter adapter = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY,
			command -> new StartResult(APPLIED, 990777, true, "old-walker"), command -> {
				cleanup.set(command);
				return APPLIED;
			}, event -> {
				dispatches.incrementAndGet();
				return new DispatchResult(DispatchResult.Status.APPLIED, DispatchResult.Propagation.STOP);
			}, (questId, resourceKey) -> released.get(), identity -> APPLIED);
		ActionInvocation invocation = invocation(2333, spawnZoneAction(), "durable-escort");
		CleanupLease plan = adapter.prepareLease(invocation);
		assertEquals(APPLIED, adapter.execute(withLease(invocation, plan)));

		EscortReachedTargetEvent wrongFollower = new EscortReachedTargetEvent("wrong", 7, 1000, 2333, 204416, 990778,
			210040000, 3);
		assertEquals(FAILED, adapter.onReached(wrongFollower));
		assertEquals(0, dispatches.get());

		EscortReachedTargetEvent reached = new EscortReachedTargetEvent("reached", 7, 1001, 2333, 204416, 990777,
			210040000, 3);
		assertEquals(FAILED, adapter.onReached(reached));
		assertEquals(1, dispatches.get());
		assertEquals(1, adapter.size());
		assertEquals(null, cleanup.get());

		released.set(true);
		assertEquals(APPLIED, adapter.onReached(reached));
		assertEquals(2, dispatches.get());
		assertEquals(CleanupReason.REACHED_TARGET, cleanup.get().reason());
		assertEquals(0, adapter.size());
	}

	@Test
	void materializedRecoveryRequiresLiveFollowerAndTaskValidation() {
		ActionInvocation invocation = invocation(2333, spawnZoneAction(), "durable-escort");
		EscortResourceIdentity identity = new EscortResourceIdentity(7, 2333, 990777, 204416, 210040000, 3,
			10, 20, 30, 0, 0, true, "old-walker", spawnZoneAction(), "durable-escort");
		QuestGraphEscortActionAdapter adapter = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY,
			command -> {
				throw new AssertionError("materialized recovery must not start a new follower");
			}, command -> APPLIED, event -> {
				throw new AssertionError("recovery does not dispatch a terminal event");
			}, (questId, resourceKey) -> false, persisted -> FAILED);

		assertEquals(FAILED, adapter.rehydrate(CleanupLease.escort(identity)));
		assertEquals(FAILED, adapter.execute(withLease(invocation, CleanupLease.escort(identity))));
		assertEquals(0, adapter.size());
	}

	@Test
	void processSessionClosesAdapterRecreationWindowBeforeMaterializedCas() {
		EscortSessionRegistry sessions = new EscortSessionRegistry();
		AtomicInteger starts = new AtomicInteger();
		ActionInvocation invocation = invocation(2333, spawnZoneAction(), "durable-escort");
		QuestGraphEscortActionAdapter first = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY, command -> {
				starts.incrementAndGet();
				return new StartResult(APPLIED, 990777, true, "old-walker");
			}, command -> APPLIED, event -> {
				throw new AssertionError("terminal dispatch is unrelated to recovery");
			}, (questId, resourceKey) -> false, identity -> APPLIED, sessions);
		CleanupLease plan = first.prepareLease(invocation);
		assertEquals(APPLIED, first.execute(withLease(invocation, plan)));
		CleanupLease materialized = first.leaseFor(invocation);

		AtomicReference<CleanupCommand> cleanup = new AtomicReference<>();
		QuestGraphEscortActionAdapter recreated = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY, command -> {
				throw new AssertionError("shared session must prevent duplicate follower creation");
			}, command -> {
				cleanup.set(command);
				return APPLIED;
			}, event -> {
				throw new AssertionError("terminal dispatch is unrelated to recovery");
			}, (questId, resourceKey) -> false, identity -> APPLIED, sessions);

		assertEquals(ALREADY_APPLIED, recreated.execute(withLease(invocation, plan)));
		assertEquals(materialized, recreated.leaseFor(invocation));
		assertEquals(1, starts.get());
		assertEquals(APPLIED, recreated.clear(materialized, CleanupReason.LOGOUT));
		assertEquals(990777, cleanup.get().followerObjectId());
		assertEquals(0, first.size());
		assertEquals(0, recreated.size());
	}

	@Test
	void processSessionReservesSinglePlayerTaskBeforeWorldSideEffects() {
		EscortSessionRegistry sessions = new EscortSessionRegistry();
		AtomicInteger secondStarts = new AtomicInteger();
		QuestGraphEscortActionAdapter second = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY, command -> {
				secondStarts.incrementAndGet();
				return new StartResult(APPLIED, 990778, true, "old-walker");
			}, command -> APPLIED, event -> {
				throw new AssertionError("terminal dispatch is unrelated to reservation");
			}, (questId, resourceKey) -> false, identity -> APPLIED, sessions);
		QuestGraphEscortActionAdapter first = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY, command -> {
				assertEquals(FAILED, second.execute(invocation(2394, spawnZoneAction(), "other-escort")));
				return new StartResult(APPLIED, 990777, true, "old-walker");
			}, command -> APPLIED, event -> {
				throw new AssertionError("terminal dispatch is unrelated to reservation");
			}, (questId, resourceKey) -> false, identity -> APPLIED, sessions);

		assertEquals(APPLIED, first.execute(invocation(2333, spawnZoneAction(), "first-escort")));
		assertEquals(0, secondStarts.get());
		assertEquals(1, first.size());
		assertEquals(APPLIED, first.onAbandon(2333));
	}

	@Test
	void durableOperationIdentityPreventsDuplicateFollowerBeforeJournalMaterialization() {
		AtomicReference<CleanupLease> durableRow = new AtomicReference<>();
		QuestGraphResourceOperationRegistry operations = new QuestGraphResourceOperationRegistry(
			(playerId, key) -> durableRow.get(), candidate -> {
				durableRow.compareAndSet(null, candidate);
				return durableRow.get();
			}, expected -> durableRow.compareAndSet(expected, null));
		Set<Integer> followers = new HashSet<>();
		AtomicInteger physicalStarts = new AtomicInteger();
		AtomicInteger reservations = new AtomicInteger();
		java.util.function.Function<StartCommand, StartResult> starter = command -> {
			boolean created = followers.add(command.followerObjectId());
			if (created) {
				physicalStarts.incrementAndGet();
			}
			return new StartResult(created ? APPLIED : ALREADY_APPLIED, command.followerObjectId(), true,
				command.previousWalkerId());
		};
		ActionInvocation invocation = invocation(2333, spawnZoneAction(), "durable-restart-window");
		QuestGraphEscortActionAdapter first = durableEscortAdapter(operations, starter, reservations);
		CleanupLease plan = first.prepareLease(invocation);

		assertEquals(APPLIED, first.execute(withLease(invocation, plan)));
		QuestGraphEscortActionAdapter recreated = durableEscortAdapter(operations, starter, reservations);
		assertEquals(ALREADY_APPLIED, recreated.execute(withLease(invocation, plan)));
		assertEquals(1, physicalStarts.get());
		assertEquals(1, reservations.get());
		assertEquals(990777,
			((EscortResourceIdentity) recreated.leaseFor(invocation).identity()).objectId());
	}

	@Test
	void objectIdReservationConflictReleasesOnlyAnAllocatedFollowerId() {
		AtomicInteger releasedId = new AtomicInteger();
		QuestGraphResourceOperationRegistry operations = new QuestGraphResourceOperationRegistry(
			(playerId, key) -> null, candidate -> {
				throw new ObjectIdReservationConflictException(new IllegalStateException("duplicate object id"));
			}, expected -> false);
		QuestGraphEscortActionAdapter adapter = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY,
			command -> {
				throw new AssertionError("escort start must not run after reservation conflict");
			}, command -> APPLIED, event -> {
				throw new AssertionError("terminal dispatch is unrelated to reservation conflict");
			}, (questId, key) -> false, identity -> APPLIED, new EscortSessionRegistry(), operations,
			() -> 990655, releasedId::set, command -> new FollowerReservation(990655, null, true));
		ActionInvocation invocation = invocation(2333, spawnZoneAction(), "object-id-conflict");

		assertEquals(FAILED, adapter.execute(withLease(invocation, adapter.prepareLease(invocation))));
		assertEquals(990655, releasedId.get());
	}

	@Test
	void missingFollowerCleanupCancelsFollowTaskBeforeLookupAndReleasesOperation() {
		List<String> calls = new ArrayList<>();
		RecordingPlayerController controller = new RecordingPlayerController(calls);
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		player.controller = controller;
		TestWorld world = new ObjenesisStd().newInstance(TestWorld.class);
		world.calls = calls;
		worldBootstrapServices = new GameWorldBootstrapServices(null, null, null, null, provider(World.class, world));

		EscortResourceIdentity identity = new EscortResourceIdentity(7, 2333, 990777, 204416, 210040000, 3,
			10, 20, 30, 0, 0, true, "old-walker", spawnZoneAction(), "missing-follower");
		CleanupLease lease = CleanupLease.escort(identity);
		AtomicReference<CleanupLease> durableRow = new AtomicReference<>(lease);
		QuestGraphResourceOperationRegistry operations = new QuestGraphResourceOperationRegistry(
			(playerId, key) -> durableRow.get(), candidate -> durableRow.get(), expected -> {
				calls.add("release");
				return durableRow.compareAndSet(expected, null);
			});
		QuestGraphEscortActionAdapter adapter = new QuestGraphEscortActionAdapter(7,
			() -> new PlayerContext(7, 210040000, 3, 10, 20, 30), command -> READY, command -> StartResult.failed(),
			command -> QuestGraphEscortActionAdapter.cleanup(player, command), event -> {
				throw new AssertionError("cleanup must not dispatch an owner event");
			}, (questId, key) -> false, persisted -> FAILED, new EscortSessionRegistry(), operations,
			() -> 0, id -> { }, command -> new FollowerReservation(0, null, false));

		assertEquals(ALREADY_APPLIED, adapter.clear(lease, CleanupReason.LOGOUT));
		assertEquals(null, durableRow.get());
		assertEquals(List.of("cancel:QUEST_FOLLOW", "find:990777", "release"), calls);
	}

	private static QuestGraphEscortActionAdapter durableEscortAdapter(QuestGraphResourceOperationRegistry operations,
			java.util.function.Function<StartCommand, StartResult> starter, AtomicInteger reservations) {
		return new QuestGraphEscortActionAdapter(7, () -> new PlayerContext(7, 210040000, 3, 10, 20, 30),
			command -> READY, starter, command -> APPLIED, event -> {
				throw new AssertionError("terminal dispatch is unrelated to operation recovery");
			}, (questId, key) -> false, identity -> APPLIED, new EscortSessionRegistry(), operations,
			() -> 990777, id -> { }, command -> {
				reservations.incrementAndGet();
				return new FollowerReservation(990777, null, true);
			});
	}

	private static StartEscortAction spawnZoneAction() {
		return new StartEscortAction(EscortSource.PLAYER_POSITION_SPAWN, 204416, (byte) 8, null, true, true, true,
			false, new EscortZoneDestination("DF2_ITEMUSEAREA_Q2333"));
	}

	private static StartEscortAction eventCoordinatesAction() {
		return new StartEscortAction(EscortSource.EVENT_NPC, 0, (byte) 0, "4212", true, false, true, false,
			new EscortCoordinatesDestination(505.69427f, 437.69382f, 885.1844f));
	}

	private static StartEscortAction replaceNpcAction() {
		return new StartEscortAction(EscortSource.REPLACE_EVENT_NPC_AT_PLAYER_POSITION, 204830, (byte) 0, null, false, true,
			false, true, new EscortNpcDestination(204828));
	}

	private static ActionInvocation invocation(int questId, StartEscortAction action, String key) {
		return invocation(questId, action, key, true);
	}

	private static ActionInvocation invocation(int questId, StartEscortAction action, String key, boolean eventObjectIdentity) {
		DialogEvent event = eventObjectIdentity
			? new DialogEvent("dialog", 7, 1_700_000_000_000L, 203709, 880001, "STEP_TO_1")
			: new DialogEvent("dialog", 7, 1_700_000_000_000L, 203709, "STEP_TO_1");
		return new ActionInvocation(action, questId, 0, QuestStatus.START, event,
			RepeatDeadlineResolution.NOT_APPLICABLE, null, key);
	}

	private static ActionInvocation withLease(ActionInvocation invocation, CleanupLease lease) {
		return new ActionInvocation(invocation.action(), invocation.questId(), invocation.actionIndex(), invocation.questStatus(),
			invocation.event(), invocation.repeatDeadlineResolution(), invocation.itemMutationPlan(),
			Map.of(invocation.idempotencyKey(), lease), invocation.idempotencyKey());
	}

	private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		return beanFactory.getBeanProvider(type);
	}

	private static final class TestWorld extends World {
		private List<String> calls;

		@Override
		public VisibleObject findVisibleObject(int objectId) {
			calls.add("find:" + objectId);
			return null;
		}
	}

	private static final class TestPlayer extends Player {
		private PlayerController controller;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public PlayerController getController() {
			return controller;
		}
	}

	private static final class RecordingPlayerController extends PlayerController {
		private final List<String> calls;

		private RecordingPlayerController(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public Future<?> cancelTask(TaskId taskId) {
			calls.add("cancel:" + taskId);
			return null;
		}
	}
}
