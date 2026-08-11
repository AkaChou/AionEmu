package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.PersistenceMode;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDsl;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestProductionDispatcherTest {
	@Test
	void ownerRouteLookupDoesNotConfuseAnotherTypedOwner() {
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition(1101)), new ArrayList<>(),
			(connection, playerId, questId, event) -> new QuestSnapshot(playerId, questId, QuestStatus.START, 0,
				Map.of()));
		QuestEvent event = new QuestEvent.TalkToNpc(203057, 1009);

		assertTrue(dispatcher.hasRoutes(event));
		assertTrue(dispatcher.hasRoutes(event, 1101));
		assertFalse(dispatcher.hasRoutes(event, 1102));
	}

	@Test
	void liveOwnerCommitsThroughRouterAndCoordinator() {
		List<String> calls = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition(1101)), calls,
			(connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, QuestStatus.START, 0, Map.of()));

		QuestEvent event = new QuestEvent.TalkToNpc(203057, 1009, 77);
		QuestEventRouter.DispatchResult result = dispatcher.dispatch(event, 7, 1101,
			QuestDispatchContract.EXCLUSIVE);

		assertTrue(result.consumed());
		assertTrue(result.claimed());
		assertEquals(List.of(1101), result.owners().stream().map(QuestEventRouter.OwnerResult::questId).toList());
		assertEquals(List.of("setAutoCommit:false", "state", "commit", "publish", "close"),
			calls);
	}

	@Test
	void typedExecutionFailureClaimsOwnerAndCannotFallBack() {
		List<String> calls = new ArrayList<>();
		List<QuestAuditEvent> audits = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition(1101)), calls,
			(connection, playerId, questId, event) -> {
				throw new SQLException("snapshot failed");
			}, audits);

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.TalkToNpc(203057, 1009), 7, 1101, QuestDispatchContract.EXCLUSIVE);

		assertFalse(result.consumed());
		assertTrue(result.claimed(), "FAILED is conclusive and must block legacy fallback");
		assertEquals(QuestRouteResult.FAILED, result.owners().get(0).result());
		assertEquals(1, audits.size());
		QuestAuditEvent audit = audits.get(0);
		assertEquals("started", audit.sourceNode());
		assertEquals("reward", audit.targetNode());
		assertEquals(203057, audit.npcId());
		assertEquals(1009, audit.dialogId());
		assertEquals(QuestFailureStage.SNAPSHOT, audit.failureStage());
		assertFalse(audit.committed());
		assertEquals(SQLException.class.getName(), audit.failureType());
		assertEquals(List.of("setAutoCommit:false", "rollback", "close"), calls);
	}

	@Test
	void explicitOwnerDispatchCannotExecuteAnotherTypedOwner() {
		List<String> calls = new ArrayList<>();
		List<Integer> snapshots = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition(1101), definition(1102)), calls,
			(connection, playerId, questId, event) -> {
				snapshots.add(questId);
				return new QuestSnapshot(playerId, questId, QuestStatus.START, 0, Map.of());
			});

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.TalkToNpc(203057, 1009), 7, 1102, QuestDispatchContract.EXCLUSIVE);

		assertTrue(result.claimed());
		assertEquals(List.of(1102), snapshots);
		assertEquals(List.of(1102), result.owners().stream().map(QuestEventRouter.OwnerResult::questId).toList());
	}

	@Test
	void killBroadcastExecutesEveryMatchingTypedOwner() {
		List<String> calls = new ArrayList<>();
		List<Integer> snapshots = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(
			List.of(killDefinition(1101), killDefinition(1102)), calls,
			(connection, playerId, questId, event) -> {
				snapshots.add(questId);
				return new QuestSnapshot(playerId, questId, QuestStatus.START, 0, Map.of());
			});

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.KillNpc(210133), 7, 0, QuestDispatchContract.BROADCAST);

		assertEquals(List.of(1101, 1102), snapshots);
		assertEquals(List.of(1101, 1102),
			result.owners().stream().map(QuestEventRouter.OwnerResult::questId).toList());
		assertTrue(result.consumed());
	}

	@Test
	void enterZoneAcceptRunsUnacceptedToStarted() {
		List<String> calls = new ArrayList<>();
		CompiledQuestDefinition definition = QuestDsl.quest(1100)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(QuestDsl.enterZone("AKARIOS_VILLAGE_210010000")).from("unaccepted").goTo("started")
			.compile();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), calls,
			(connection, playerId, questId, event) -> new QuestSnapshot(playerId, questId, QuestStatus.NONE, 0,
				Map.of()).withStartEligibility(QuestStartEligibility.allowed()));

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.EnterZone("AKARIOS_VILLAGE_210010000"), 7, 0, QuestDispatchContract.BROADCAST);

		assertTrue(result.consumed());
		assertTrue(result.claimed());
		assertEquals(List.of(1100), result.owners().stream().map(QuestEventRouter.OwnerResult::questId).toList());
		assertEquals(List.of("setAutoCommit:false", "state", "commit", "publish", "close"), calls);
	}

	@Test
	void levelUpAcceptRunsUnacceptedToStarted() {
		List<String> calls = new ArrayList<>();
		CompiledQuestDefinition definition = QuestDsl.quest(1001)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(QuestDsl.levelUp()).from("unaccepted").goTo("started")
			.compile();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), calls,
			(connection, playerId, questId, event) -> new QuestSnapshot(playerId, questId, QuestStatus.NONE, 0,
				Map.of()).withStartEligibility(QuestStartEligibility.allowed()));

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.LevelUp(), 7, 0, QuestDispatchContract.BROADCAST);

		assertTrue(result.consumed());
		assertTrue(result.claimed());
		assertEquals(List.of(1001), result.owners().stream().map(QuestEventRouter.OwnerResult::questId).toList());
	}

	@Test
	void unrelatedEventDoesNotAcquireDatabaseConnection() {
		AtomicInteger connections = new AtomicInteger();
		CompiledQuestDefinition definition = definition(1101);
		QuestProductionDispatcher dispatcher = new QuestProductionDispatcher(
			new ImmutableQuestCatalog(List.of(definition)), new QuestExecutionCoordinator(new PlayerSerialExecutor()),
			(connection, playerId, questId, event) -> {
				throw new AssertionError("snapshot must not run");
			}, noOpActions(), noOpState(), (action, snapshot, plan) -> { },
			() -> {
				connections.incrementAndGet();
				return connection(new ArrayList<>());
			}, ignored -> { }, new QuestRuntimeMetricsCollector());

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.TalkToNpc(203049, 31), 7, 1101, QuestDispatchContract.EXCLUSIVE);

		assertFalse(result.claimed());
		assertTrue(result.owners().isEmpty());
		assertEquals(0, connections.get());
	}

	@Test
	void sharedQuestAcceptRunsTheTypedAcquisitionActionsWithoutAnNpcTarget() {
		CompiledQuestDefinition definition = QuestDsl.quest(28738)
			.progress(bitField("var0", 0, 2, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.TalkToNpc(206395, 1002)).from("unaccepted")
			.then(QuestDsl.giveItem(164000342, 10)).goTo("started")
			.compile();
		List<String> calls = new ArrayList<>();
		List<QuestEvent> events = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), calls,
			(connection, playerId, questId, event) -> {
				events.add(event);
				return new QuestSnapshot(playerId, questId, QuestStatus.NONE, 0, Map.of())
					.withStartEligibility(QuestStartEligibility.allowed());
			});

		assertFalse(dispatcher.dispatchSharedQuestAccept(7, 28738, 20000));
		assertFalse(dispatcher.dispatchSharedQuestAccept(7, 28738, 31));
		assertTrue(calls.isEmpty());
		assertTrue(dispatcher.dispatchSharedQuestAccept(7, 28738, 1002));

		assertEquals(List.of(new QuestEvent.QuestDialog(1002)), events);
		assertEquals(List.of("setAutoCommit:false", "preflight", "apply", "state", "commit", "publish", "close"),
			calls);
	}

	@Test
	void firstNonUnknownContinuesPastAnUnmatchedTransitionOfTheSameOwner() {
		CompiledQuestDefinition definition = QuestDsl.quest(1107)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.UseItem(182200501)).from("started").goTo("started")
			.on(new QuestEvent.LevelUp()).from("started").goTo("reward")
			.on(new QuestEvent.UseItem(182200501)).from("reward").goTo("reward")
			.compile();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), new ArrayList<>(),
			(connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, QuestStatus.REWARD, 1, Map.of()));

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.UseItem(182200501, 900008), 7, 0, QuestDispatchContract.FIRST_NON_UNKNOWN);

		assertTrue(result.consumed());
		assertEquals(List.of(1107, 1107), result.owners().stream()
			.map(QuestEventRouter.OwnerResult::questId).toList());
	}

	@Test
	void blockDefaultItemUseReturnsAConclusiveBlockingRoute() {
		CompiledQuestDefinition definition = QuestDsl.quest(1109)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.on(new QuestEvent.UseItem(182200501)).from("started")
			.then(QuestDsl.blockDefaultItemUse()).goTo("started")
			.compile();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), new ArrayList<>(),
			(connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, QuestStatus.START, 0, Map.of()));

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.UseItem(182200501), 7, 0, QuestDispatchContract.FIRST_NON_UNKNOWN);

		assertFalse(result.consumed());
		assertTrue(result.claimed());
		assertEquals(QuestRouteResult.BLOCKED, result.owners().get(0).result());
	}

	@Test
	void itemPlayCommitsItsDelayedUseMutationThroughTheCentralCoordinator() {
		CompiledQuestDefinition definition = QuestDsl.quest(1561)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, Map.of("var0", 0)))
			.node("started", project(QuestStatus.START, Map.of("var0", 0)))
			.on(new QuestEvent.ItemPlay(182201728, 3000)).from("unaccepted")
			.then(QuestDsl.removeItem(182201728, 1)).goTo("started").compile();
		List<String> calls = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), calls,
			(connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, QuestStatus.NONE, 0, Map.of(182201728, 1))
					.withStartEligibility(QuestStartEligibility.allowed()));

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.ItemPlay(182201728, 3000), 7, 0, QuestDispatchContract.FIRST_NON_UNKNOWN);

		assertTrue(result.consumed());
		assertTrue(result.claimed());
		assertEquals(List.of("setAutoCommit:false", "preflight", "apply", "state", "commit", "publish", "close"),
			calls);
	}

	@Test
	void failCraftCommitsTheTypedRollbackRoute() {
		CompiledQuestDefinition definition = QuestDsl.quest(19038)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("failed", project(QuestStatus.START, vars("var0", 2)))
			.node("retry", project(QuestStatus.START, vars("var0", 1)))
			.on(new QuestEvent.FailCraft(182206773)).from("failed")
			.then(QuestDsl.setVariable("var0", 1)).goTo("retry")
			.compile();
		List<String> calls = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), calls,
			(connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, QuestStatus.START, 2, Map.of()));

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.FailCraft(182206773), 7, 0, QuestDispatchContract.BROADCAST);

		assertTrue(result.consumed());
		assertTrue(result.claimed());
		assertEquals(List.of("setAutoCommit:false", "state", "commit", "publish", "close"), calls);
	}

	@Test
	void sameNpcDifferentDialogIsUnknownAndFallsThroughToMatchingRoute() {
		CompiledQuestDefinition definition = QuestDsl.quest(1108)
			.progress(bitField("step", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("step", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("step", 1)))
			.on(new QuestEvent.TalkToNpc(798125, 31)).from("started").goTo("reward")
			.on(new QuestEvent.TalkToNpc(798125, -1)).from("reward").goTo("reward")
			.afterCommit(QuestDsl.showQuestDialog(1352))
			.compile();
		List<String> calls = new ArrayList<>();
		QuestProductionDispatcher dispatcher = dispatcher(List.of(definition), calls,
			(connection, playerId, questId, event) ->
				new QuestSnapshot(playerId, questId, QuestStatus.REWARD, 1, Map.of()));

		QuestEventRouter.DispatchResult result = dispatcher.dispatch(
			new QuestEvent.TalkToNpc(798125, -1), 7, 0, QuestDispatchContract.EXCLUSIVE);

		assertTrue(result.consumed());
		assertTrue(result.claimed());
		assertEquals(List.of(QuestRouteResult.UNKNOWN, QuestRouteResult.HANDLED),
			result.owners().stream().map(QuestEventRouter.OwnerResult::result).toList());
		assertEquals(List.of("setAutoCommit:false", "commit", "close"), calls);
	}

	private static QuestProductionDispatcher dispatcher(List<CompiledQuestDefinition> definitions,
			List<String> calls, QuestEventPort eventPort) {
		return dispatcher(definitions, calls, eventPort, new ArrayList<>());
	}

	private static QuestProductionDispatcher dispatcher(List<CompiledQuestDefinition> definitions,
			List<String> calls, QuestEventPort eventPort, List<QuestAuditEvent> audits) {
		return new QuestProductionDispatcher(new ImmutableQuestCatalog(definitions),
			new QuestExecutionCoordinator(new PlayerSerialExecutor()), eventPort, recordingActions(calls),
			recordingState(calls), (action, snapshot, plan) -> { }, () -> connection(calls),
			audits::add, new QuestRuntimeMetricsCollector());
	}

	private static CompiledQuestDefinition definition(int id) {
		return QuestDsl.quest(id)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(new QuestEvent.TalkToNpc(203057, 1009)).from("started").goTo("reward")
			.compile();
	}

	private static CompiledQuestDefinition killDefinition(int id) {
		return QuestDsl.quest(id)
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("started", project(QuestStatus.START, vars("var0", 0)))
			.node("one-kill", project(QuestStatus.START, vars("var0", 1)))
			.on(new QuestEvent.KillNpc(210133)).from("started").goTo("one-kill")
			.compile();
	}

	private static QuestActionPort recordingActions(List<String> calls) {
		return new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
				calls.add("preflight");
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> actions) {
				calls.add("apply");
				return QuestTransactionParticipant.none();
			}
		};
	}

	private static QuestActionPort noOpActions() {
		return new QuestActionPort() {
			@Override
			public void preflight(Connection connection, QuestSnapshot snapshot, List<QuestAction> actions) {
			}

			@Override
			public QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
					List<QuestAction> actions) {
				return QuestTransactionParticipant.none();
			}
		};
	}

	private static QuestStatePort recordingState(List<String> calls) {
		return new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
				calls.add("state");
			}

			@Override
			public void publish(int playerId, QuestMutationPlan plan) {
				calls.add("publish");
			}
		};
	}

	private static QuestStatePort noOpState() {
		return new QuestStatePort() {
			@Override
			public void apply(Connection connection, int playerId, QuestMutationPlan plan) {
			}

			@Override
			public void publish(int playerId, QuestMutationPlan plan) {
			}
		};
	}

	private static Connection connection(List<String> calls) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
			(proxy, method, args) -> switch (method.getName()) {
				case "getAutoCommit" -> true;
				case "setAutoCommit" -> { calls.add("setAutoCommit:" + args[0]); yield null; }
				case "commit" -> { calls.add("commit"); yield null; }
				case "rollback" -> { calls.add("rollback"); yield null; }
				case "close" -> { calls.add("close"); yield null; }
				default -> method.getReturnType() == boolean.class ? false : null;
			});
	}
}
