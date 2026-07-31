package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DelayItemUseContinuationAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.QuestGraphDefinitionRegistry;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ContinuationCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemUseContinuationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

class QuestGraphItemUseContinuationRecoveryCoordinatorTest {

	private static final int PLAYER_ID = 7;
	private static final int ITEM_ID = 182206034;
	private static final int ITEM_OBJECT_ID = 55;
	private static final int DURATION_MS = 3000;
	private static final long OCCURRED_AT = 1_700_000_000_000L;
	private static final ZoneId SERVER_ZONE = ZoneId.of("UTC");

	@Test
	void loginRecoveryScansEveryPreparedContinuation() {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Fixture first = fixture(1, states);
		Fixture second = fixture(2, states);
		QuestGraphDefinitionRegistry registry = registry(first.graph(), second.graph());
		AtomicInteger contexts = new AtomicInteger();
		AtomicInteger delays = new AtomicInteger();
		BiFunction<CompiledQuestGraph, Function<ContinuationCommand, ActionResult>, TransitionContext> contextFactory = (graph, resumer) -> {
			contexts.incrementAndGet();
			return context(PLAYER_ID, states, invocation -> {
				delays.incrementAndGet();
				return ActionResult.DEFERRED;
			}, (expected, next) -> PersistenceResult.APPLIED);
		};
		QuestGraphItemUseContinuationRecoveryCoordinator coordinator = new QuestGraphItemUseContinuationRecoveryCoordinator(
			PLAYER_ID, states, registry, new QuestGraphTransitionExecutor(), contextFactory);

		assertEquals(ActionResult.APPLIED, coordinator.recoverOnLogin());
		assertEquals(2, contexts.get());
		assertEquals(2, delays.get());
		assertEquals(Lifecycle.PREPARED, states.get(1).getLifecycle());
		assertEquals(Lifecycle.PREPARED, states.get(2).getLifecycle());
	}

	@Test
	void loginRecoveryAlsoResumesTailAfterBarrierWasPersisted() {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Fixture fixture = fixture(1, states, 1);
		AtomicInteger protocolActions = new AtomicInteger();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(fixture.state());
		QuestGraphItemUseContinuationRecoveryCoordinator coordinator = new QuestGraphItemUseContinuationRecoveryCoordinator(
			PLAYER_ID, states, registry(fixture.graph()), new QuestGraphTransitionExecutor(),
			(graph, resumer) -> context(PLAYER_ID, states, invocation -> {
				protocolActions.incrementAndGet();
				return ActionResult.APPLIED;
			}, cas(database)));

		assertEquals(ActionResult.APPLIED, coordinator.recoverOnLogin());
		assertEquals(1, protocolActions.get());
		assertEquals(Lifecycle.ACTIVE, states.get(1).getLifecycle());
		assertEquals(QuestStatus.START, states.get(1).getQuestStatus());
	}

	@Test
	void exactCallbackResumesTailOnceAndStaleCallbackDoesNothing() {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Fixture fixture = fixture(1, states);
		QuestGraphDefinitionRegistry registry = registry(fixture.graph());
		AtomicInteger actions = new AtomicInteger();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(fixture.state());
		TransitionContext context = context(PLAYER_ID, states, invocation -> {
			actions.incrementAndGet();
			return ActionResult.APPLIED;
		}, cas(database));
		QuestGraphItemUseContinuationRecoveryCoordinator coordinator = new QuestGraphItemUseContinuationRecoveryCoordinator(
			PLAYER_ID, states, registry, new QuestGraphTransitionExecutor(), (graph, resumer) -> context);

		assertEquals(ActionResult.APPLIED, coordinator.resume(fixture.command()));
		assertEquals(2, actions.get());
		assertEquals(Lifecycle.ACTIVE, states.get(1).getLifecycle());
		assertEquals("done", states.get(1).getNodeId());
		assertEquals(QuestStatus.START, states.get(1).getQuestStatus());
		assertEquals(ActionResult.ALREADY_APPLIED, coordinator.resume(fixture.command()));
		assertEquals(2, actions.get());
	}

	@Test
	void wrongPlayerAndJournalIdentityFailClosedBeforeRecovery() {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Fixture fixture = fixture(1, states);
		AtomicInteger actions = new AtomicInteger();
		QuestGraphItemUseContinuationRecoveryCoordinator coordinator = new QuestGraphItemUseContinuationRecoveryCoordinator(
			PLAYER_ID, states, registry(fixture.graph()), new QuestGraphTransitionExecutor(),
			(graph, resumer) -> context(PLAYER_ID, states, invocation -> {
				actions.incrementAndGet();
				return ActionResult.DEFERRED;
			}, (expected, next) -> PersistenceResult.APPLIED));

		ContinuationCommand wrongPlayer = command(fixture, PLAYER_ID + 1, fixture.command().baseRevision(), ITEM_OBJECT_ID);
		ContinuationCommand staleRevision = command(fixture, PLAYER_ID, fixture.command().baseRevision() + 1, ITEM_OBJECT_ID);
		ContinuationCommand wrongItemObject = command(fixture, PLAYER_ID, fixture.command().baseRevision(), ITEM_OBJECT_ID + 1);
		ContinuationCommand wrongKey = new ContinuationCommand(fixture.command().questId(), PLAYER_ID,
			fixture.command().baseRevision(), fixture.command().transitionId(), fixture.command().actionIndex(),
			fixture.command().eventId(), fixture.command().itemId(), fixture.command().itemObjectId(), fixture.command().durationMs(),
			fixture.command().readyAt(), "wrong-key");
		assertEquals(ActionResult.FAILED, coordinator.resume(wrongPlayer));
		assertEquals(ActionResult.ALREADY_APPLIED, coordinator.resume(staleRevision));
		assertEquals(ActionResult.ALREADY_APPLIED, coordinator.resume(wrongItemObject));
		assertEquals(ActionResult.ALREADY_APPLIED, coordinator.resume(wrongKey));
		assertEquals(0, actions.get());
	}

	@Test
	void contextMustOwnExactPlayerAndStateList() {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Fixture fixture = fixture(1, states);
		QuestGraphDefinitionRegistry registry = registry(fixture.graph());
		PlayerQuestGraphStateList otherStates = new PlayerQuestGraphStateList();
		Function<ActionInvocation, ActionResult> deferred = invocation -> ActionResult.DEFERRED;
		QuestGraphItemUseContinuationRecoveryCoordinator wrongPlayerContext = new QuestGraphItemUseContinuationRecoveryCoordinator(
			PLAYER_ID, states, registry, new QuestGraphTransitionExecutor(),
			(graph, resumer) -> context(PLAYER_ID + 1, states, deferred, (expected, next) -> PersistenceResult.APPLIED));
		QuestGraphItemUseContinuationRecoveryCoordinator wrongStateContext = new QuestGraphItemUseContinuationRecoveryCoordinator(
			PLAYER_ID, states, registry, new QuestGraphTransitionExecutor(),
			(graph, resumer) -> context(PLAYER_ID, otherStates, deferred, (expected, next) -> PersistenceResult.APPLIED));

		assertThrows(IllegalStateException.class, wrongPlayerContext::recoverOnLogin);
		assertThrows(IllegalStateException.class, wrongStateContext::recoverOnLogin);
	}

	@Test
	void preparedStateCannotBeOverwrittenByNormalExecute() {
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Fixture fixture = fixture(1, states);
		AtomicInteger actions = new AtomicInteger();
		Transition transition = fixture.graph().nodes().get("offer").transitions().getFirst();
		ItemUseEvent replay = new ItemUseEvent(fixture.command().eventId(), PLAYER_ID, OCCURRED_AT, ITEM_ID, ITEM_OBJECT_ID);
		TransitionContext context = context(PLAYER_ID, states, invocation -> {
			actions.incrementAndGet();
			return ActionResult.APPLIED;
		}, (expected, next) -> PersistenceResult.APPLIED);

		assertEquals(DispatchResult.Status.FAILED, new QuestGraphTransitionExecutor().execute(
			new Match(replay, fixture.graph(), new EventRoute(1, "offer", transition), fixture.state()), context));
		assertEquals(0, actions.get());
		assertEquals(fixture.state(), states.get(1));
	}

	private static Fixture fixture(int questId, PlayerQuestGraphStateList states) {
		return fixture(questId, states, 0);
	}

	private static Fixture fixture(int questId, PlayerQuestGraphStateList states, int nextActionIndex) {
		String transitionId = "item-use-" + questId;
		String eventId = "item-use-event-" + questId;
		ItemUseEvent event = new ItemUseEvent(eventId, PLAYER_ID, OCCURRED_AT, ITEM_ID, ITEM_OBJECT_ID);
		Transition transition = new Transition(transitionId, 10, "done", new Event(EventType.ITEM_USE, ITEM_ID, null), List.of(),
			List.of(new DelayItemUseContinuationAction(DURATION_MS), new SetQuestStatusAction(QuestStatus.START), new PlayMovieAction(913)));
		CompiledQuestGraph graph = new CompiledQuestGraph(questId, 1, StateScope.PLAYER, "offer", Map.of(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		ItemUseContinuationPlan plan = new ItemUseContinuationPlan(0, ITEM_ID, ITEM_OBJECT_ID, DURATION_MS,
			OCCURRED_AT + DURATION_MS, 1, 2);
		PreparedTransition journal = new PreparedTransition(-1, eventId, transitionId, nextActionIndex, false,
			RepeatDeadlineResolution.NOT_APPLICABLE, Map.of(), Map.of(), Map.of(0, plan), Map.of(), new byte[0],
			QuestGraphEventCodec.encode(event));
		PlayerQuestGraphState state = new PlayerQuestGraphState(questId, 1, 0, "offer", QuestStatus.NONE, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), journal, Map.of(), null);
		states.addLoaded(state);
		String key = QuestGraphTransitionExecutor.actionIdempotencyKey(eventId, questId, transitionId, PLAYER_ID, 0);
		ContinuationCommand command = new ContinuationCommand(questId, PLAYER_ID, -1, transitionId, 0, eventId,
			ITEM_ID, ITEM_OBJECT_ID, DURATION_MS, OCCURRED_AT + DURATION_MS, key);
		return new Fixture(graph, state, command);
	}

	private static QuestGraphDefinitionRegistry registry(CompiledQuestGraph... graphs) {
		Map<Integer, CompiledQuestGraph> byQuest = java.util.Arrays.stream(graphs)
			.collect(java.util.stream.Collectors.toUnmodifiableMap(CompiledQuestGraph::questId, Function.identity()));
		QuestGraphDefinitionRegistry registry = new QuestGraphDefinitionRegistry();
		registry.installInitial(new CompiledQuestGraphData(byQuest, Map.of()));
		return registry;
	}

	private static TransitionContext context(int playerId, PlayerQuestGraphStateList states,
			Function<ActionInvocation, ActionResult> actions,
			java.util.function.BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
		return new TransitionContext(playerId, 0, SERVER_ZONE, states,
			invocation -> QuestGraphTransitionExecutor.ConditionResult.MATCHED,
			invocation -> QuestGraphTransitionExecutor.PreflightResult.READY, actions, persistence);
	}

	private static java.util.function.BiFunction<Long, PlayerQuestGraphState, PersistenceResult> cas(
			AtomicReference<PlayerQuestGraphState> database) {
		return (expectedRevision, next) -> {
			PlayerQuestGraphState current = database.get();
			if (expectedRevision == null ? current != null : current == null || current.getRevision() != expectedRevision) {
				return PersistenceResult.CONFLICT;
			}
			database.set(next);
			return PersistenceResult.APPLIED;
		};
	}

	private static ContinuationCommand command(Fixture fixture, int playerId, long baseRevision, int itemObjectId) {
		ContinuationCommand original = fixture.command();
		return new ContinuationCommand(original.questId(), playerId, baseRevision, original.transitionId(), original.actionIndex(),
			original.eventId(), original.itemId(), itemObjectId, original.durationMs(), original.readyAt(), original.idempotencyKey());
	}

	private record Fixture(CompiledQuestGraph graph, PlayerQuestGraphState state, ContinuationCommand command) {
	}
}
