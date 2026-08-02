package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.EvidenceRef;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.completeQuest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.hasItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.removeItem;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.syncQuestState;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRuntimeInfrastructureTest {
	private static final EvidenceRef EVIDENCE = new EvidenceRef("test", "runtime", "fixture");

	@Test
	void indexIsDeterministicAndPlannerHasNoSideEffects() {
		CompiledQuestDefinition definition = definition(1001);
		ImmutableQuestCatalog catalog = new ImmutableQuestCatalog(List.of(definition));
		QuestEvent event = talkToNpc(700001);
		QuestEventIndex index = new QuestEventIndex(catalog);
		QuestEventIndex.Route route = index.routesFor(event).get(0);
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(182400001, 5));

		var plan = QuestMutationPlanner.plan(definition, snapshot, event, route.transition());
		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.REWARD, plan.orElseThrow().nextStatus());
		assertEquals(1, plan.orElseThrow().nextPackedVariables());
		assertTrue(QuestMutationPlanner.plan(definition, snapshot, new QuestEvent.KillNpc(1), route.transition()).isEmpty());
		assertTrue(QuestMutationPlanner.plan(definition,
				new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of()), event, route.transition()).isEmpty());
	}

	@Test
	void plannerRequiresSnapshotToMatchDeclaredSourceNode() {
		CompiledQuestDefinition definition = quest(1001)
			.evidence(EVIDENCE)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(talkToNpc(700001)).from("start").goTo("reward").compile();
		var transition = definition.definition().transitions().get(0);

		assertTrue(QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of()), transition).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 1001, QuestStatus.START, 1, Map.of()), transition).isEmpty());
	}

	@Test
	void eventIndexOrdersOverlappingRoutesByOwnerThenPriority() {
		var builder = quest(1001).evidence(EVIDENCE)
			.node("start", project(QuestStatus.START, Map.of()));
		builder.on(talkToNpc(700001)).from("start").priority(20).goTo("start");
		builder.on(talkToNpc(700001)).from("start").priority(10).goTo("start");

		List<Integer> priorities = new QuestEventIndex(new ImmutableQuestCatalog(List.of(builder.compile())))
			.routesFor(talkToNpc(700001)).stream().map(route -> route.transition().priority()).toList();

		assertEquals(List.of(10, 20), priorities);
	}

	@Test
	void rankedThresholdsShareOnePvpRoute() {
		CompiledQuestDefinition rankThree = quest(3741).evidence(EVIDENCE)
			.node("start", project(QuestStatus.START, Map.of()))
			.on(new QuestEvent.KillRanked(3)).from("start").goTo("start").compile();
		CompiledQuestDefinition rankEight = quest(3742).evidence(EVIDENCE)
			.node("start", project(QuestStatus.START, Map.of()))
			.on(new QuestEvent.KillRanked(8)).from("start").goTo("start").compile();

		List<QuestEventIndex.Route> routes = new QuestEventIndex(new ImmutableQuestCatalog(
			List.of(rankThree, rankEight))).routesFor(new QuestEvent.KillRanked(12));
		assertEquals(List.of(3741, 3742), routes.stream().map(QuestEventIndex.Route::questId).toList());
	}

	@Test
	void broadcastContinuesAfterOneOwnerFails() {
		QuestEvent event = talkToNpc(700001);
		QuestEventIndex index = new QuestEventIndex(new ImmutableQuestCatalog(List.of(definition(1001), definition(1002))));
		List<QuestAuditEvent> audits = new ArrayList<>();
		QuestRuntimeMetricsCollector metrics = new QuestRuntimeMetricsCollector();
		QuestEventRouter router = new QuestEventRouter(index, audits::add, metrics);
		QuestEventRouter.DispatchResult result = router.dispatch(event, QuestDispatchContract.BROADCAST, route -> {
			if (route.questId() == 1001) {
				throw new IllegalStateException("owner failed");
			}
			return QuestRouteResult.HANDLED;
		});

		assertEquals(List.of(1001, 1002), result.owners().stream().map(QuestEventRouter.OwnerResult::questId).toList());
		assertEquals(QuestRouteResult.FAILED, result.owners().get(0).result());
		assertTrue(result.consumed());
		assertEquals(1, audits.size());
		assertEquals(1001, audits.get(0).questId());
		assertEquals(QuestDispatchContract.BROADCAST, audits.get(0).contract());
		assertEquals(1, metrics.snapshot().outcomeCount(QuestRouteResult.FAILED));
		assertEquals(1, metrics.snapshot().outcomeCount(QuestRouteResult.HANDLED));
	}

	@Test
	void firstNonUnknownPreservesCallerContract() {
		QuestEvent event = talkToNpc(700001);
		QuestEventRouter router = new QuestEventRouter(new QuestEventIndex(
				new ImmutableQuestCatalog(List.of(definition(1001), definition(1002)))),
				ignored -> { }, new QuestRuntimeMetricsCollector());
		AtomicInteger calls = new AtomicInteger();
		QuestEventRouter.DispatchResult result = router.dispatch(event, QuestDispatchContract.FIRST_NON_UNKNOWN, route -> {
			calls.incrementAndGet();
			return route.questId() == 1001 ? QuestRouteResult.UNKNOWN : QuestRouteResult.NOT_HANDLED;
		});
		assertEquals(2, calls.get());
		assertEquals(2, result.owners().size());
	}

	@Test
	void exclusiveContinuesOnUnmatchedAndStopsOnFirstConclusion() {
		QuestEvent event = talkToNpc(700001);
		QuestEventRouter router = new QuestEventRouter(new QuestEventIndex(
				new ImmutableQuestCatalog(List.of(definition(1001), definition(1002), definition(1003)))),
				ignored -> { }, new QuestRuntimeMetricsCollector());
		List<Integer> called = new ArrayList<>();
		QuestEventRouter.DispatchResult result = router.dispatch(event, QuestDispatchContract.EXCLUSIVE, route -> {
			called.add(route.questId());
			return route.questId() == 1002 ? QuestRouteResult.HANDLED : QuestRouteResult.NOT_HANDLED;
		});

		assertEquals(List.of(1001, 1002), called);
		assertEquals(List.of(1001, 1002), result.owners().stream()
				.map(QuestEventRouter.OwnerResult::questId).toList());
		assertTrue(result.consumed());
	}

	@Test
	void broadcastStopsAdditionalTransitionsForOneOwnerButContinuesOtherOwners() {
		var builder = QuestDsl.quest(1001);
		builder.evidence(EVIDENCE)
				.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("step", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
				.node("complete", project(QuestStatus.COMPLETE, vars("step", 2)));
		builder.on(talkToNpc(700001)).from("start").when(statusIs(QuestStatus.START))
				.then(setVariable("step", 1)).goTo("reward");
		builder.on(talkToNpc(700001)).from("reward").when(statusIs(QuestStatus.REWARD))
			.then(setVariable("step", 2)).then(completeQuest(0)).goTo("complete")
			.afterCommit(syncQuestState(QuestStateSyncMode.COMPLETION));
		CompiledQuestDefinition firstOwner = builder.compile();

		QuestEventRouter router = new QuestEventRouter(new QuestEventIndex(new ImmutableQuestCatalog(
				List.of(firstOwner, definition(1002)))), ignored -> { }, new QuestRuntimeMetricsCollector());
		List<Integer> called = new ArrayList<>();
		QuestEventRouter.DispatchResult result = router.dispatch(talkToNpc(700001), QuestDispatchContract.BROADCAST,
				route -> {
					called.add(route.questId());
					return route.questId() == 1001 ? QuestRouteResult.HANDLED : QuestRouteResult.NOT_HANDLED;
				});

		assertEquals(List.of(1001, 1002), called);
		assertEquals(List.of(1001, 1002), result.owners().stream()
				.map(QuestEventRouter.OwnerResult::questId).toList());
	}

	@Test
	void auditSinkFailureIsolatedFromBroadcast() {
		QuestEvent event = talkToNpc(700001);
		QuestRuntimeMetricsCollector metrics = new QuestRuntimeMetricsCollector();
		QuestEventRouter router = new QuestEventRouter(new QuestEventIndex(
				new ImmutableQuestCatalog(List.of(definition(1001), definition(1002)))),
			ignored -> { throw new IllegalStateException("audit unavailable"); }, metrics);
		QuestEventRouter.DispatchResult result = router.dispatch(event, QuestDispatchContract.BROADCAST, route -> {
			if (route.questId() == 1001) {
				throw new IllegalStateException("owner failed");
			}
			return QuestRouteResult.HANDLED;
		});

		assertEquals(List.of(1001, 1002), result.owners().stream().map(QuestEventRouter.OwnerResult::questId).toList());
		assertEquals(1, metrics.snapshot().auditFailures());
		assertEquals(1, metrics.snapshot().outcomeCount(QuestRouteResult.HANDLED));
	}

	@Test
	void playerEventsAreSerialized() throws Exception {
		PlayerSerialExecutor executor = new PlayerSerialExecutor();
		AtomicInteger value = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(2);
		Future<?> first = pool.submit(() -> repeat(executor, value));
		Future<?> second = pool.submit(() -> repeat(executor, value));
		first.get();
		second.get();
		pool.shutdownNow();
		assertEquals(200, value.get());
		// 引用计数锁:最后一次执行结束后锁被驱逐,不为历史 player 永久保留
		assertEquals(0, executor.trackedPlayers());
	}

	@Test
	void serialExecutorReleasesLockAfterLastExecution() throws Exception {
		PlayerSerialExecutor executor = new PlayerSerialExecutor();

		executor.execute(7, () -> null);

		assertEquals(0, executor.trackedPlayers());
		// 同一玩家再次执行仍串行可用(锁按需重建)
		executor.execute(7, () -> null);
		assertEquals(0, executor.trackedPlayers());
	}

	@Test
	void serialExecutorKeepsLockWhileConcurrentExecutionsAreActive() throws Exception {
		PlayerSerialExecutor executor = new PlayerSerialExecutor();
		AtomicInteger value = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(2);
		java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);

		Future<?> first = pool.submit(() -> {
			try {
				executor.execute(7, () -> {
					value.incrementAndGet();
					entered.countDown();
					release.await();
					return null;
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		entered.await();
		// 执行进行中:锁仍在 map,阻止第二个并发执行
		assertEquals(1, executor.trackedPlayers());
		release.countDown();
		first.get();
		pool.shutdownNow();
		assertEquals(0, executor.trackedPlayers());
	}

	private static void repeat(PlayerSerialExecutor executor, AtomicInteger value) {
		for (int i = 0; i < 100; i++) {
			try {
				executor.execute(7, () -> {
					int current = value.get();
					Thread.yield();
					value.set(current + 1);
					return null;
				});
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}
	}

	private static CompiledQuestDefinition definition(int id) {
		return quest(id)
				.evidence(EVIDENCE)
				.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
				.node("start", project(QuestStatus.START, vars("var1", 0)))
				.node("reward", project(QuestStatus.REWARD, vars("var1", 1)))
				.on(talkToNpc(700001)).when(statusIs(QuestStatus.START)).when(hasItem(182400001, 5))
				.then(removeItem(182400001, 5)).then(setVariable("var1", 1)).goTo("reward").compile();
	}
}
