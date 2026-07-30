package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.ITEM_DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.MOVIE_ENDED;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus.NONE;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope.PLAYER;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope.WORLD;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.ALREADY_APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.NOT_MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult.CONFLICT;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.READY;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.REJECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AddCompletionCountAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AddQuestVariableAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AnchoredCooldownRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AbandonQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.BooleanVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CraftSkillEligibilityCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.CraftSkillEligibilityPolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DailyRepeatDeadlinePolicy;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EndQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IncrementPackedCounterAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.FinishQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GiveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GrantCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestCompletionCountCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRewardCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestRepeatAvailableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestVariableCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PackedCounterCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveCollectedItemsAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendDialogAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SendRepeatDeadlineMessageAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SpawnInstanceNpcAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetCompletionCountAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestVariableAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncCraftSkillRewardAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.TeleportPlayerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCraftSkillReferenceCatalog;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SyncQuestTimerAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RepeatTimeBasis;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemDialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.MovieEndedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ScheduleResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.PreparedTransition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineDisposition;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;
import com.aionemu.gameserver.questEngine.model.ConditionOperation;

import jakarta.xml.bind.JAXBContext;

/**
 * 验证任务图转换的预检、CAS、journal 推进和恢复重放。
 * Verifies quest-graph transition preflight, CAS, journal progress, and recovery replay.
 */
class QuestGraphTransitionExecutorTest {

	private static final DialogEvent EVENT = new DialogEvent("dialog-1", 7, 1000, 100, 900100, "QUEST_SELECT");
	private static final QuestGraphCraftSkillReferenceCatalog CRAFT_REFERENCES = craftReferences();
	private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Shanghai");
	private final QuestGraphTransitionExecutor executor = new QuestGraphTransitionExecutor();

	/**
	 * 验证新任务从无状态开始，初始化变量并最终提交目标节点。
	 * Verifies that a new quest initializes variables and commits its target node from no state.
	 */
	@Test
	void newQuestInitializesAndCommitsAfterAction() {
		Fixture fixture = fixture();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		AtomicReference<ActionInvocation> action = new AtomicReference<>();
		TransitionContext context = context(fixture.states(), database, invocation -> {
			action.set(invocation);
			return APPLIED;
		});

		assertEquals(DispatchResult.Status.APPLIED, executor.execute(fixture.match(), context));

		PlayerQuestGraphState state = fixture.states().get(1);
		assertEquals(3, state.getRevision());
		assertEquals("done", state.getNodeId());
		assertEquals(Lifecycle.ACTIVE, state.getLifecycle());
		assertEquals(CompiledQuestGraph.QuestStatus.START, state.getQuestStatus());
		assertNull(state.getJournal());
		assertEquals(new IntValue(2), state.getVariables().get("count"));
		assertEquals(new BooleanValue(false), state.getVariables().get("enabled"));
		assertEquals("8:dialog-1:1:accept:7:1", action.get().idempotencyKey());
		assertEquals(state, database.get());
	}

	/**
	 * 验证 packed counter 在同一状态归约中执行 63 到 64 的跨位进位，并以解码总值评估条件。
	 * Verifies that a packed counter carries atomically from 63 to 64 and evaluates conditions against the decoded total.
	 */
	@Test
	void packedCounterConditionAndIncrementAreAtomic() {
		Map<String, CompiledQuestGraph.Variable> variables = new LinkedHashMap<>();
		variables.put("digit0", new IntVariable("digit0", PLAYER, 0, 0, 63));
		variables.put("digit1", new IntVariable("digit1", PLAYER, 0, 0, 63));
		Transition transition = new Transition("count", 10, "active", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(
			new QuestStatusCondition(CompiledQuestGraph.QuestStatus.START),
			new PackedCounterCondition(List.of("digit0", "digit1"), 64, ConditionOperation.LESSER, 100)), List.of(
				new IncrementPackedCounterAction(List.of("digit0", "digit1"), 64, 100),
				new SyncQuestStatusAction()));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", variables,
			Map.of("active", new Node("active", false, List.of(transition))));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		PlayerQuestGraphState current = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of("digit0", new IntValue(63), "digit1", new IntValue(0)),
			Map.of(), null, Map.of(), null);
		states.addLoaded(current);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(current);
		Match match = new Match(EVENT, graph, new EventRoute(1, "active", transition), current);

		assertEquals(DispatchResult.Status.APPLIED,
			executor.execute(match, context(states, database, invocation -> APPLIED)));
		assertEquals(new IntValue(0), states.get(1).getVariables().get("digit0"));
		assertEquals(new IntValue(1), states.get(1).getVariables().get("digit1"));
		assertEquals(states.get(1), database.get());
	}

	/**
	 * 验证条件不匹配和动作预检拒绝都不产生写入或副作用。
	 * Verifies that condition mismatch and action preflight rejection produce no write or side effect.
	 */
	@Test
	void mismatchAndPreflightRejectionDoNotWrite() {
		Fixture mismatch = fixture();
		AtomicInteger mismatchActions = new AtomicInteger();
		AtomicInteger mismatchWrites = new AtomicInteger();
		TransitionContext mismatchContext = new TransitionContext(7, 0, SERVER_ZONE, mismatch.states(), invocation -> NOT_MATCHED, invocation -> READY,
			invocation -> {
				mismatchActions.incrementAndGet();
				return APPLIED;
			}, (expected, state) -> {
				mismatchWrites.incrementAndGet();
				return PersistenceResult.APPLIED;
			});
		assertEquals(DispatchResult.Status.NO_MATCH, executor.execute(mismatch.match(), mismatchContext));
		assertEquals(0, mismatchActions.get());
		assertEquals(0, mismatchWrites.get());

		Fixture rejected = fixture();
		AtomicInteger rejectedActions = new AtomicInteger();
		AtomicInteger rejectedWrites = new AtomicInteger();
		TransitionContext rejectedContext = new TransitionContext(7, 0, SERVER_ZONE, rejected.states(), invocation -> MATCHED, invocation -> REJECTED,
			invocation -> {
				rejectedActions.incrementAndGet();
				return APPLIED;
			}, (expected, state) -> {
				rejectedWrites.incrementAndGet();
				return PersistenceResult.APPLIED;
			});
		assertEquals(DispatchResult.Status.REJECTED, executor.execute(rejected.match(), rejectedContext));
		assertEquals(0, rejectedActions.get());
		assertEquals(0, rejectedWrites.get());
	}

	/**
	 * 验证 canonical graph 状态会在外部 condition callback 前阻断状态不匹配。
	 * Verifies that canonical graph state blocks a status mismatch before the external condition callback.
	 */
	@Test
	void canonicalQuestStatusCannotBeOverriddenByConditionCallback() {
		Fixture fixture = fixture();
		PlayerQuestGraphState active = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		fixture.states().addLoaded(active);
		AtomicInteger callbacks = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> {
			callbacks.incrementAndGet();
			return MATCHED;
		}, invocation -> READY, invocation -> APPLIED, (expected, state) -> PersistenceResult.APPLIED);
		Match match = new Match(EVENT, fixture.graph(), fixture.match().route(), active);

		assertEquals(DispatchResult.Status.NO_MATCH, executor.execute(match, context));
		assertEquals(0, callbacks.get());
		assertEquals(active, fixture.states().get(1));
	}

	/**
	 * 验证跨任务状态、奖励和完成次数只读 canonical 状态，且不可被外部 callback 覆盖。
	 * Verifies that cross-quest status, reward, and completion count read canonical state and cannot be overridden by callbacks.
	 */
	@Test
	void canonicalPrerequisitesReadOnlyGraphState() {
		List<Condition> conditions = List.of(new QuestStatusCondition(NONE),
			new QuestStatusCondition(2, ConditionOperation.IN, Set.of(CompiledQuestGraph.QuestStatus.COMPLETE)),
			new QuestRewardCondition(2, 1), new QuestCompletionCountCondition(2, ConditionOperation.EQUAL, 3));
		Fixture matched = fixture(conditions);
		matched.states().addLoaded(new PlayerQuestGraphState(2, 1, 0, "done", CompiledQuestGraph.QuestStatus.COMPLETE,
			new QuestHistory(3, 1, 1_700_000_000_000L, null), null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null));
		AtomicInteger callbacks = new AtomicInteger();
		TransitionContext matchedContext = new TransitionContext(7, 0, SERVER_ZONE, matched.states(), invocation -> {
			callbacks.incrementAndGet();
			return NOT_MATCHED;
		}, invocation -> READY, invocation -> APPLIED, (expected, state) -> PersistenceResult.APPLIED);

		assertEquals(DispatchResult.Status.APPLIED, executor.execute(matched.match(), matchedContext));
		assertEquals(0, callbacks.get());

		Fixture missingReward = fixture(List.of(new QuestStatusCondition(NONE), new QuestRewardCondition(2, 0)));
		assertEquals(DispatchResult.Status.NO_MATCH, executor.execute(missingReward.match(),
			new TransitionContext(7, 0, SERVER_ZONE, missingReward.states(), invocation -> MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));

		Fixture missingQuest = fixture(List.of(new QuestStatusCondition(NONE),
			new QuestStatusCondition(2, ConditionOperation.IN, Set.of(NONE, CompiledQuestGraph.QuestStatus.LOCKED)),
			new QuestCompletionCountCondition(2, ConditionOperation.EQUAL, 0)));
		assertEquals(DispatchResult.Status.APPLIED, executor.execute(missingQuest.match(),
			new TransitionContext(7, 0, SERVER_ZONE, missingQuest.states(), invocation -> NOT_MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));

		Fixture quarantinedQuest = fixture(List.of(new QuestStatusCondition(NONE),
			new QuestStatusCondition(2, ConditionOperation.IN, Set.of(CompiledQuestGraph.QuestStatus.START))));
		quarantinedQuest.states().addLoaded(new PlayerQuestGraphState(2, 1, 0, "start", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.QUARANTINED, Map.of(), Map.of(), null, Map.of(), "recovery failed"));
		assertEquals(DispatchResult.Status.FAILED, executor.execute(quarantinedQuest.match(),
			new TransitionContext(7, 0, SERVER_ZONE, quarantinedQuest.states(), invocation -> MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));
	}

	/**
	 * 验证重复资格可显式匹配“可用”和“不可用”，缺失必需 deadline 时仍失败。
	 * Verifies explicit available/unavailable repeat matching while a required missing deadline still fails.
	 */
	@Test
	void canonicalRepeatAvailabilitySupportsExplicitNegation() {
		QuestHistory waiting = new QuestHistory(1, 0, 900L, 2000L);
		Fixture unavailable = fixture(List.of(new QuestRepeatAvailableCondition(255, true, false)));
		PlayerQuestGraphState waitingState = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.COMPLETE,
			waiting, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		unavailable.states().addLoaded(waitingState);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, unavailable.states(), invocation -> NOT_MATCHED, invocation -> READY,
			invocation -> APPLIED, (expected, state) -> PersistenceResult.APPLIED);
		assertEquals(DispatchResult.Status.APPLIED,
			executor.execute(new Match(EVENT, unavailable.graph(), unavailable.match().route(), waitingState), context));

		Fixture available = fixture(List.of(new QuestRepeatAvailableCondition(255, true, true)));
		PlayerQuestGraphState future = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.COMPLETE,
			waiting, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		available.states().addLoaded(future);
		assertEquals(DispatchResult.Status.NO_MATCH, executor.execute(
			new Match(EVENT, available.graph(), available.match().route(), future),
			new TransitionContext(7, 0, SERVER_ZONE, available.states(), invocation -> MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));

		Fixture missingDeadline = fixture(List.of(new QuestRepeatAvailableCondition(255, true, false)));
		PlayerQuestGraphState missing = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.COMPLETE,
			new QuestHistory(1, 0, 900L, null), null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		missingDeadline.states().addLoaded(missing);
		assertEquals(DispatchResult.Status.FAILED, executor.execute(
			new Match(EVENT, missingDeadline.graph(), missingDeadline.match().route(), missing),
			new TransitionContext(7, 0, SERVER_ZONE, missingDeadline.states(), invocation -> MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));

		Fixture bypassedDeadline = fixture(List.of(new QuestRepeatAvailableCondition(255, true, true)));
		PlayerQuestGraphState bypassed = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.COMPLETE,
			new QuestHistory(1, 0, 900L, null, RepeatDeadlineDisposition.PRIVILEGED_BYPASS), null, Lifecycle.ACTIVE, Map.of(), Map.of(), null,
			Map.of(), null);
		bypassedDeadline.states().addLoaded(bypassed);
		assertEquals(DispatchResult.Status.APPLIED, executor.execute(
			new Match(EVENT, bypassedDeadline.graph(), bypassedDeadline.match().route(), bypassed),
			new TransitionContext(7, 0, SERVER_ZONE, bypassedDeadline.states(), invocation -> MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));
	}

	/**
	 * 验证 canonical 变量命中，并在 ACTIVE 状态缺失声明变量时显式失败。
	 * Verifies a canonical variable match and explicit failure when ACTIVE state lacks the declared variable.
	 */
	@Test
	void canonicalQuestVariableRejectsCorruptActiveState() {
		List<Condition> conditions = List.of(new QuestVariableCondition("count", ConditionOperation.EQUAL, 2));
		Fixture matched = fixture(conditions);
		PlayerQuestGraphState valid = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of("count", new IntValue(2), "enabled", new BooleanValue(false)),
			Map.of(), null, Map.of(), null);
		matched.states().addLoaded(valid);
		assertEquals(DispatchResult.Status.APPLIED, executor.execute(
			new Match(EVENT, matched.graph(), matched.match().route(), valid),
			new TransitionContext(7, 0, SERVER_ZONE, matched.states(), invocation -> NOT_MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));

		Fixture corrupt = fixture(conditions);
		PlayerQuestGraphState missing = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		corrupt.states().addLoaded(missing);
		assertEquals(DispatchResult.Status.FAILED, executor.execute(
			new Match(EVENT, corrupt.graph(), corrupt.match().route(), missing),
			new TransitionContext(7, 0, SERVER_ZONE, corrupt.states(), invocation -> MATCHED, invocation -> READY, invocation -> APPLIED,
				(expected, state) -> PersistenceResult.APPLIED)));
	}

	/**
	 * 验证已知非法 canonical action 状态在 PREPARED 和 callback 前失败。
	 * Verifies that a known-invalid canonical action state fails before PREPARED and callbacks.
	 */
	@Test
	void illegalCanonicalActionStateFailsDuringPreflight() {
		Fixture fixture = fixture();
		Transition original = fixture.match().route().transition();
		Transition transition = new Transition(original.id(), original.priority(), original.targetNode(), original.event(), List.of(),
			original.actions());
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", fixture.graph().variables(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.REWARD, QuestHistory.EMPTY, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		fixture.states().addLoaded(state);
		AtomicInteger callbacks = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> MATCHED, invocation -> {
			callbacks.incrementAndGet();
			return READY;
		}, invocation -> APPLIED, (expected, next) -> PersistenceResult.APPLIED);

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "offer", transition), state), context));
		assertEquals(0, callbacks.get());
		assertEquals(state, fixture.states().get(1));
	}

	/**
	 * 验证 LOCKED 不能保留既有完成历史，且在 lifecycle 外部预检、执行和持久化前关闭失败。
	 * Verifies LOCKED cannot retain completion history and fails closed before lifecycle preflight, execution, or persistence.
	 */
	@Test
	void lockedStatusWithCompletionHistoryFailsBeforeLifecycleSideEffects() {
		Fixture fixture = fixture();
		Transition original = fixture.match().route().transition();
		Transition transition = new Transition(original.id(), original.priority(), original.targetNode(), original.event(), List.of(),
			List.of(new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.LOCKED), new AbandonQuestAction()));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", fixture.graph().variables(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		QuestHistory history = new QuestHistory(2, 1, 1_700_000_000_000L, null);
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.START, history, null,
			Lifecycle.ACTIVE, Map.of("count", new IntValue(0), "enabled", new BooleanValue(false)), Map.of(), null, Map.of(), null);
		fixture.states().addLoaded(state);
		AtomicInteger preflights = new AtomicInteger();
		AtomicInteger executions = new AtomicInteger();
		AtomicInteger writes = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> MATCHED, invocation -> {
			preflights.incrementAndGet();
			return READY;
		}, invocation -> {
			executions.incrementAndGet();
			return APPLIED;
		}, (expected, next) -> {
			writes.incrementAndGet();
			return PersistenceResult.APPLIED;
		});

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "offer", transition), state), context));
		assertEquals(0, preflights.get());
		assertEquals(0, executions.get());
		assertEquals(0, writes.get());
		assertEquals(state, fixture.states().get(1));
	}

	/**
	 * 验证重复接取从 COMPLETE 进入 START 时保留 canonical 历史。
	 * Verifies that repeat acceptance preserves canonical history while moving from COMPLETE to START.
	 */
	@Test
	void repeatedStartPreservesCanonicalHistory() {
		Fixture fixture = fixture();
		Transition original = fixture.match().route().transition();
		Transition transition = new Transition(original.id(), original.priority(), original.targetNode(), original.event(), List.of(),
			original.actions());
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", fixture.graph().variables(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		QuestHistory history = new QuestHistory(2, 1, 1_700_000_000_000L, 1_750_000_000_000L);
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.COMPLETE, history, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		fixture.states().addLoaded(state);
		Match match = new Match(EVENT, graph, new EventRoute(1, "offer", transition), state);
		TransitionContext context = context(fixture.states(), new AtomicReference<>(state), invocation -> APPLIED);

		assertEquals(DispatchResult.Status.APPLIED, executor.execute(match, context));
		assertEquals(CompiledQuestGraph.QuestStatus.START, fixture.states().get(1).getQuestStatus());
		assertEquals(history, fixture.states().get(1).getHistory());
	}

	/**
	 * 验证动作失败保留可恢复 PREPARED journal 且不推进节点。
	 * Verifies that action failure retains a recoverable PREPARED journal without advancing the node.
	 */
	@Test
	void actionFailureKeepsPreparedJournal() {
		Fixture fixture = fixture();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		TransitionContext context = context(fixture.states(), database, invocation -> FAILED);

		assertEquals(DispatchResult.Status.FAILED, executor.execute(fixture.match(), context));

		PlayerQuestGraphState state = fixture.states().get(1);
		assertEquals(0, state.getRevision());
		assertEquals("offer", state.getNodeId());
		assertEquals(Lifecycle.PREPARED, state.getLifecycle());
		assertEquals(CompiledQuestGraph.QuestStatus.NONE, state.getQuestStatus());
		assertEquals(-1, state.getJournal().getBaseRevision());
		assertEquals(0, state.getJournal().getNextActionIndex());
	}

	/**
	 * 验证状态动作内部归约、必需副作用 journal 和提交后协议严格分阶段执行。
	 * Verifies staged state reduction, required-effect journaling, and post-commit protocol ordering.
	 */
	@Test
	void standardDialogLifecycleStagesStateRequiredEffectsAndProtocol() {
		Map<String, CompiledQuestGraph.Variable> variables = Map.of("var0", new IntVariable("var0", PLAYER, 0, 0, 5));
		List<Action> actions = List.of(
			new SetQuestVariableAction("var0", 1),
			new AddQuestVariableAction("var0", 1),
			new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.REWARD),
			new RemoveCollectedItemsAction(),
			new FinishQuestAction(0),
			new SyncQuestStatusAction(),
			new SendDialogAction(5));
		Transition transition = new Transition("finish", 10, "done", new Event(DIALOG, 100, "SELECT_REWARD"),
			List.of(new QuestStatusCondition(CompiledQuestGraph.QuestStatus.START)), actions);
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", variables,
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of("var0", new IntValue(0)), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		List<CompiledQuestGraph.ActionType> preflighted = new java.util.ArrayList<>();
		List<CompiledQuestGraph.ActionType> executed = new java.util.ArrayList<>();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> {
			preflighted.add(invocation.action().type());
			return READY;
		}, invocation -> {
			executed.add(invocation.action().type());
			if (invocation.action().type().phase() == CompiledQuestGraph.ActionPhase.POST_COMMIT_PROTOCOL) {
				assertEquals(Lifecycle.PREPARED, states.get(1).getLifecycle());
				assertEquals("done", states.get(1).getNodeId());
				assertTrue(states.get(1).getJournal().isTargetCommitted());
				if (invocation.action() instanceof SyncQuestStatusAction) {
					throw new IllegalStateException("protocol endpoint failed");
				}
				return FAILED;
			}
			return APPLIED;
		}, cas(database));
		DialogEvent event = new DialogEvent("finish-1", 7, 1_700_000_000_000L, 100, "SELECT_REWARD");

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(event, graph, new EventRoute(1, "active", transition), initial), context));

		PlayerQuestGraphState completed = states.get(1);
		assertEquals(7, completed.getRevision());
		assertEquals(CompiledQuestGraph.QuestStatus.COMPLETE, completed.getQuestStatus());
		assertEquals(new IntValue(0), completed.getVariables().get("var0"));
		assertEquals(new QuestHistory(1, 0, 1_700_000_000_000L, null), completed.getHistory());
		assertEquals(Lifecycle.PREPARED, completed.getLifecycle());
		assertTrue(completed.getJournal().isTargetCommitted());
		assertEquals(5, completed.getJournal().getNextActionIndex());
		assertEquals(List.of(CompiledQuestGraph.ActionType.REMOVE_COLLECTED_ITEMS, CompiledQuestGraph.ActionType.FINISH_QUEST), preflighted);
		assertEquals(List.of(CompiledQuestGraph.ActionType.REMOVE_COLLECTED_ITEMS, CompiledQuestGraph.ActionType.FINISH_QUEST,
			CompiledQuestGraph.ActionType.SYNC_QUEST_STATUS), executed);

		List<CompiledQuestGraph.ActionType> recoveredProtocols = new java.util.ArrayList<>();
		TransitionContext recovery = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY, invocation -> {
			recoveredProtocols.add(invocation.action().type());
			return APPLIED;
		}, cas(database));
		assertEquals(DispatchResult.Status.APPLIED, executor.recover(graph, recovery));
		assertEquals(List.of(CompiledQuestGraph.ActionType.SYNC_QUEST_STATUS, CompiledQuestGraph.ActionType.SEND_DIALOG), recoveredProtocols);
		assertEquals(Lifecycle.ACTIVE, states.get(1).getLifecycle());
		assertNull(states.get(1).getJournal());
		assertEquals(10, states.get(1).getRevision());
	}

	/** 验证通用 action endpoint 不能用普通 APPLIED 绕过 terminal lifecycle 的逐 lease 物理清理。 / Verifies a generic action endpoint cannot bypass per-lease terminal cleanup with a plain APPLIED result. */
	@Test
	void terminalLifecycleRequiresExplicitCleanupProofBeforeLedgerReduction() {
		String resourceKey = "persisted-spawn";
		CleanupLease lease = CleanupLease.instanceSpawn(new InstanceSpawnResourceIdentity(7, 1, 990001, 216608,
			SpawnPlacementKind.FIXED, 0, 0, 210010000, 1, 10, 20, 30, (byte) 0, resourceKey));
		Map<String, CleanupLease> leases = Map.of(resourceKey, lease);
		Transition transition = new Transition("finish-with-resource", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(),
			List.of(new FinishQuestAction(0)));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.REWARD,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, leases, null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> APPLIED, cas(database));

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		PlayerQuestGraphState interrupted = states.get(1);
		assertEquals(Lifecycle.PREPARED, interrupted.getLifecycle());
		assertEquals(0, interrupted.getJournal().getNextActionIndex());
		assertEquals(CompiledQuestGraph.QuestStatus.REWARD, interrupted.getQuestStatus());
		assertEquals(leases, interrupted.getCleanupLeases());
		assertEquals(interrupted, database.get());
	}

	/**
	 * 验证 deadline 在副作用前冻结，CAS 冲突恢复时即使上下文时区和权限变化也不重算。
	 * Verifies deadline freezing before side effects and no recalculation after zone/access changes during CAS recovery.
	 */
	@Test
	void repeatDeadlineIsFrozenAcrossRecovery() {
		DailyRepeatDeadlinePolicy policy = new DailyRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, 9);
		List<Action> actions = List.of(new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.REWARD), new FinishQuestAction(2, policy),
			new SyncQuestStatusAction(), new SendRepeatDeadlineMessageAction(policy));
		Transition transition = new Transition("finish", 10, "done", new Event(DIALOG, 100, "SELECT_REWARD"),
			List.of(new QuestStatusCondition(CompiledQuestGraph.QuestStatus.START)), actions);
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicInteger writes = new AtomicInteger();
		Set<String> effects = new HashSet<>();
		List<RepeatDeadlineResolution> observed = new java.util.ArrayList<>();
		BiFunction<Long, PlayerQuestGraphState, PersistenceResult> databaseCas = cas(database);
		java.util.function.Function<ActionInvocation, QuestGraphTransitionExecutor.ActionResult> action = invocation -> {
			if (invocation.action() instanceof FinishQuestAction || invocation.action() instanceof SendRepeatDeadlineMessageAction) {
				observed.add(invocation.repeatDeadlineResolution());
			}
			return effects.add(invocation.idempotencyKey()) ? APPLIED : ALREADY_APPLIED;
		};
		TransitionContext firstContext = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY, action,
			(expected, state) -> writes.incrementAndGet() == 3 ? CONFLICT : databaseCas.apply(expected, state));
		long occurredAt = ZonedDateTime.parse("2026-07-28T10:00+08:00[Asia/Shanghai]").toInstant().toEpochMilli();
		DialogEvent event = new DialogEvent("repeat-finish", 7, occurredAt, 100, "SELECT_REWARD");

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(event, graph, new EventRoute(1, "active", transition), initial), firstContext));
		RepeatDeadlineResolution frozen = states.get(1).getJournal().getRepeatDeadlineResolution();
		assertEquals(RepeatDeadlineResolution.deadline(
			ZonedDateTime.parse("2026-07-29T09:00+08:00[Asia/Shanghai]").toInstant().toEpochMilli()), frozen);

		TransitionContext recoveryContext = new TransitionContext(7, 1, ZoneId.of("UTC"), states, invocation -> MATCHED, invocation -> READY,
			action, databaseCas);
		assertEquals(DispatchResult.Status.APPLIED, executor.recover(graph, recoveryContext));
		assertEquals(List.of(frozen, frozen, frozen), observed);
		assertEquals(new QuestHistory(1, 2, occurredAt, frozen.deadlineAt(), RepeatDeadlineDisposition.DEADLINE),
			states.get(1).getHistory());
	}

	/** 验证 privileged daily 完成会持久化显式绕过而非损坏的空 deadline。 / Verifies privileged daily completion persists an explicit bypass rather than a corrupt null deadline. */
	@Test
	void privilegedDailyFinishPersistsBypass() {
		DailyRepeatDeadlinePolicy policy = new DailyRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, 9);
		List<Action> actions = List.of(new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.REWARD), new FinishQuestAction(0, policy),
			new SendRepeatDeadlineMessageAction(policy));
		Transition transition = new Transition("finish", 10, "done", new Event(DIALOG, 100, "SELECT_REWARD"), List.of(), actions);
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicReference<RepeatDeadlineResolution> messageResolution = new AtomicReference<>();
		TransitionContext context = new TransitionContext(7, 1, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY, invocation -> {
			if (invocation.action() instanceof SendRepeatDeadlineMessageAction) {
				messageResolution.set(invocation.repeatDeadlineResolution());
			}
			return APPLIED;
		}, cas(database));
		long occurredAt = ZonedDateTime.parse("2026-07-28T10:00+08:00[Asia/Shanghai]").toInstant().toEpochMilli();

		assertEquals(DispatchResult.Status.APPLIED,
			executor.execute(new Match(new DialogEvent("gm-finish", 7, occurredAt, 100, "SELECT_REWARD"), graph,
				new EventRoute(1, "active", transition), initial), context));
		assertEquals(new QuestHistory(1, 0, occurredAt, null, RepeatDeadlineDisposition.PRIVILEGED_BYPASS), states.get(1).getHistory());
		assertEquals(RepeatDeadlineResolution.PRIVILEGED_BYPASS, messageResolution.get());
	}

	/** 验证 deadline 溢出会在任何持久化或动作副作用前失败。 / Verifies deadline overflow fails before persistence or action side effects. */
	@Test
	void repeatDeadlineOverflowFailsBeforeActions() {
		AnchoredCooldownRepeatDeadlinePolicy policy = new AnchoredCooldownRepeatDeadlinePolicy(RepeatTimeBasis.SERVER_LOCAL, Long.MAX_VALUE, 9);
		Transition transition = new Transition("finish", 10, "done", new Event(DIALOG, 100, "SELECT_REWARD"), List.of(),
			List.of(new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.REWARD), new FinishQuestAction(0, policy)));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicInteger actions = new AtomicInteger();
		AtomicInteger writes = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY, invocation -> {
			actions.incrementAndGet();
			return APPLIED;
		}, (expected, state) -> {
			writes.incrementAndGet();
			return PersistenceResult.APPLIED;
		});

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertEquals(0, actions.get());
		assertEquals(0, writes.get());
		assertEquals(initial, states.get(1));
	}

	/**
	 * 验证准备状态 CAS 冲突会在任何动作执行前阻断转换。
	 * Verifies that a PREPARED-state CAS conflict blocks the transition before any action executes.
	 */
	@Test
	void prepareCasConflictBlocksActions() {
		Fixture fixture = fixture();
		AtomicInteger actions = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> MATCHED, invocation -> READY, invocation -> {
			actions.incrementAndGet();
			return APPLIED;
		}, (expected, state) -> CONFLICT);

		assertEquals(DispatchResult.Status.FAILED, executor.execute(fixture.match(), context));
		assertEquals(0, actions.get());
		assertNull(fixture.states().get(1));
	}

	/**
	 * 验证 PLAYER executor 会在 callback 前拒绝跨 scope 变量。
	 * Verifies that the PLAYER executor rejects cross-scope variables before callbacks.
	 */
	@Test
	void crossScopeVariablesAreRejected() {
		Fixture base = fixture();
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer",
			Map.of("world", new IntVariable("world", WORLD, 0, 0, 1)), base.graph().nodes());
		Match match = new Match(EVENT, graph, new EventRoute(1, "offer", graph.nodes().get("offer").transitions().getFirst()), null);
		AtomicInteger callbacks = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, base.states(), invocation -> {
			callbacks.incrementAndGet();
			return MATCHED;
		}, invocation -> READY, invocation -> APPLIED, (expected, state) -> PersistenceResult.APPLIED);

		assertEquals(DispatchResult.Status.FAILED, executor.execute(match, context));
		assertEquals(0, callbacks.get());
		assertNull(base.states().get(1));
	}

	/**
	 * 验证事件玩家与状态 owner 不一致时不会调用能力或持久化。
	 * Verifies that an event-player/state-owner mismatch invokes neither capabilities nor persistence.
	 */
	@Test
	void eventPlayerMustMatchStateOwner() {
		Fixture fixture = fixture();
		DialogEvent foreignEvent = new DialogEvent("dialog-foreign", 8, 1000, 100, "QUEST_SELECT");
		Match foreignMatch = new Match(foreignEvent, fixture.graph(), fixture.match().route(), null);
		AtomicInteger callbacks = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> {
			callbacks.incrementAndGet();
			return MATCHED;
		}, invocation -> READY, invocation -> APPLIED, (expected, state) -> PersistenceResult.APPLIED);

		assertEquals(DispatchResult.Status.FAILED, executor.execute(foreignMatch, context));
		assertEquals(0, callbacks.get());
		assertNull(fixture.states().get(1));
	}

	/**
	 * 验证定义版本不兼容会通过 CAS 将 PREPARED 状态持久化为稳定隔离状态。
	 * Verifies an incompatible definition version is persisted as a stable quarantined state through CAS.
	 */
	@Test
	void unrecoverablePreparedStateIsPersistentlyQuarantined() {
		Fixture fixture = fixture();
		PlayerQuestGraphState prepared = preparedState(fixture.graph());
		fixture.states().addLoaded(prepared);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(prepared);
		CompiledQuestGraph replacement = new CompiledQuestGraph(1, 2, PLAYER, fixture.graph().initialNode(), fixture.graph().variables(),
			fixture.graph().nodes());
		TransitionContext context = context(fixture.states(), database, invocation -> APPLIED);

		assertEquals(DispatchResult.Status.FAILED, executor.recover(replacement, context));
		PlayerQuestGraphState quarantined = fixture.states().get(1);
		assertEquals(Lifecycle.QUARANTINED, quarantined.getLifecycle());
		assertEquals(1, quarantined.getRevision());
		assertEquals("RECOVERY_DEFINITION_VERSION_MISMATCH", quarantined.getQuarantineReason());
		assertEquals(quarantined, database.get());
	}

	/**
	 * 验证隔离 CAS 冲突保持原 PREPARED 状态，不产生内存伪成功。
	 * Verifies a quarantine CAS conflict preserves the PREPARED state without an in-memory false success.
	 */
	@Test
	void quarantineCasConflictPreservesPreparedState() {
		Fixture fixture = fixture();
		PlayerQuestGraphState prepared = preparedState(fixture.graph());
		fixture.states().addLoaded(prepared);
		CompiledQuestGraph replacement = new CompiledQuestGraph(1, 2, PLAYER, fixture.graph().initialNode(), fixture.graph().variables(),
			fixture.graph().nodes());
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> MATCHED, invocation -> READY,
			invocation -> APPLIED, (expected, state) -> CONFLICT);

		assertEquals(DispatchResult.Status.FAILED, executor.recover(replacement, context));
		assertEquals(prepared, fixture.states().get(1));
		assertEquals(Lifecycle.PREPARED, fixture.states().get(1).getLifecycle());
	}

	/**
	 * 验证已持久化 journal 的纯状态归约不再合法时会被隔离。
	 * Verifies a persisted journal is quarantined when its pure state reduction is no longer valid.
	 */
	@Test
	void recoveryQuarantinesInvalidStateTransition() {
		List<Action> actions = List.of(new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.COMPLETE), new SetCompletionCountAction(0));
		Transition transition = new Transition("invalid", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(), actions);
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PreparedTransition journal = new PreparedTransition(-1, EVENT.eventId(), "invalid", 0, QuestGraphEventCodec.encode(EVENT));
		PlayerQuestGraphState prepared = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.PREPARED, Map.of(), Map.of(), journal, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(prepared);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(prepared);

		assertEquals(DispatchResult.Status.FAILED, executor.recover(graph, context(states, database, invocation -> APPLIED)));
		assertEquals(Lifecycle.QUARANTINED, states.get(1).getLifecycle());
		assertEquals("RECOVERY_STATE_TRANSITION_INVALID", states.get(1).getQuarantineReason());
		assertEquals(states.get(1), database.get());
	}

	/**
	 * 验证恢复时物品对话事件不能跨任务 owner 重放。
	 * Verifies that item-dialog events cannot be replayed across quest owners during recovery.
	 */
	@Test
	void recoveryQuarantinesCrossOwnerItemDialogEvent() {
		Transition transition = new Transition("accept-item", 10, "active",
			new Event(ITEM_DIALOG, 182200001, "ACCEPT_QUEST"), List.of(), List.of(new StartQuestAction()));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", Map.of(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "active", new Node("active", true, List.of())));
		ItemDialogEvent event = new ItemDialogEvent("item-dialog", 7, 1000, 2, 182200001, 5001,
			"ACCEPT_QUEST", 91);
		PreparedTransition journal = new PreparedTransition(-1, event.eventId(), transition.id(), 0,
			QuestGraphEventCodec.encode(event));
		PlayerQuestGraphState prepared = new PlayerQuestGraphState(1, 1, 0, "offer", NONE, QuestHistory.EMPTY, null,
			Lifecycle.PREPARED, Map.of(), Map.of(), journal, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(prepared);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(prepared);

		assertEquals(DispatchResult.Status.FAILED, executor.recover(graph, context(states, database, invocation -> APPLIED)));
		assertEquals(Lifecycle.QUARANTINED, states.get(1).getLifecycle());
		assertEquals("RECOVERY_EVENT_INCOMPATIBLE", states.get(1).getQuarantineReason());
	}

	/**
	 * 验证外部能力的瞬时 preflight 失败保留 PREPARED 状态供后续重试。
	 * Verifies a transient external preflight failure preserves PREPARED state for a later retry.
	 */
	@Test
	void recoveryPreservesPreparedStateOnTransientPreflightFailure() {
		Fixture fixture = fixture();
		PlayerQuestGraphState prepared = preparedState(fixture.graph());
		fixture.states().addLoaded(prepared);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(prepared);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> MATCHED,
			invocation -> QuestGraphTransitionExecutor.PreflightResult.FAILED, invocation -> APPLIED, cas(database));

		assertEquals(DispatchResult.Status.FAILED, executor.recover(fixture.graph(), context));
		assertEquals(prepared, fixture.states().get(1));
		assertEquals(prepared, database.get());
	}

	/**
	 * 验证动作后 journal CAS 冲突可用相同幂等键重放并完成最终提交。
	 * Verifies replay with the same idempotency key and final commit after a post-action journal CAS conflict.
	 */
	@Test
	void recoveryReplaysIdempotentlyAndCommits() {
		Fixture fixture = fixture();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		AtomicInteger writes = new AtomicInteger();
		AtomicInteger calls = new AtomicInteger();
		Set<String> effects = new HashSet<>();
		BiFunction<Long, PlayerQuestGraphState, PersistenceResult> cas = cas(database);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> MATCHED, invocation -> READY, invocation -> {
			calls.incrementAndGet();
			return effects.add(invocation.idempotencyKey()) ? APPLIED : ALREADY_APPLIED;
		}, (expected, state) -> writes.incrementAndGet() == 3 ? CONFLICT : cas.apply(expected, state));

		assertEquals(DispatchResult.Status.FAILED, executor.execute(fixture.match(), context));
		assertEquals(Lifecycle.PREPARED, fixture.states().get(1).getLifecycle());
		assertEquals(CompiledQuestGraph.QuestStatus.START, fixture.states().get(1).getQuestStatus());
		assertEquals(DispatchResult.Status.APPLIED, executor.recover(fixture.graph(), context));

		PlayerQuestGraphState committed = fixture.states().get(1);
		assertEquals(3, calls.get());
		assertEquals(2, effects.size());
		assertEquals(3, committed.getRevision());
		assertEquals("done", committed.getNodeId());
		assertEquals(Lifecycle.ACTIVE, committed.getLifecycle());
		assertEquals(CompiledQuestGraph.QuestStatus.START, committed.getQuestStatus());
		assertEquals(committed, database.get());
	}

	/**
	 * 验证物品已同步持久化但 journal CAS 失败时，恢复会使用冻结计划且不重复发放。
	 * Verifies recovery uses the frozen plan without a duplicate grant after inventory persistence but journal CAS failure.
	 */
	@Test
	void itemMutationRecoveryDoesNotDuplicateGrant() {
		GiveQuestItemAction give = new GiveQuestItemAction(182200001, 5, CompiledQuestGraph.QuestItemGrantMode.TOP_UP_TO);
		Transition transition = new Transition("accept", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"),
			List.of(new QuestStatusCondition(NONE)), List.of(new StartQuestAction(), give));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", Map.of(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Match match = new Match(EVENT, graph, new EventRoute(1, "offer", transition), null);
		AtomicLong itemCount = new AtomicLong(2);
		AtomicInteger grants = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter items = new QuestGraphItemActionAdapter(7, new Object(), ignored -> itemCount.get(), values -> true,
			(itemId, delta) -> {
				grants.incrementAndGet();
				itemCount.addAndGet(delta);
				return true;
			}, (itemId, delta) -> false, () -> {
				stores.incrementAndGet();
				return true;
			}, itemId -> true);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		AtomicInteger writes = new AtomicInteger();
		BiFunction<Long, PlayerQuestGraphState, PersistenceResult> databaseCas = cas(database);
		TransitionContext first = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> APPLIED, items, (expected, state) -> writes.incrementAndGet() == 3 ? CONFLICT : databaseCas.apply(expected, state));

		assertEquals(DispatchResult.Status.FAILED, executor.execute(match, first));
		assertEquals(Lifecycle.PREPARED, states.get(1).getLifecycle());
		assertEquals(1, states.get(1).getJournal().getNextActionIndex());
		assertEquals(ItemMutationKind.GIVE_TOP_UP_TO, states.get(1).getJournal().getItemMutationPlans().get(1).kind());
		assertEquals(5, itemCount.get());
		assertEquals(1, grants.get());

		TransitionContext recovery = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> APPLIED, items, databaseCas);
		assertEquals(DispatchResult.Status.APPLIED, executor.recover(graph, recovery));
		assertEquals(1, grants.get());
		assertEquals(2, stores.get());
		assertEquals(Lifecycle.ACTIVE, states.get(1).getLifecycle());
		assertEquals("done", states.get(1).getNodeId());
	}

	/** 验证 guard 后出现的库存不足作为业务拒绝返回，且不会写 PREPARED。 / Verifies post-guard insufficiency rejects without PREPARED. */
	@Test
	void insufficientExactRemoveRejectsBeforePrepared() {
		RemoveQuestItemAction remove = new RemoveQuestItemAction(182200001, 2,
			CompiledQuestGraph.QuestItemRemovalMode.EXACT);
		Transition transition = new Transition("remove", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"),
			List.of(new QuestStatusCondition(NONE)), List.of(new StartQuestAction(), remove));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", Map.of(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Match match = new Match(EVENT, graph, new EventRoute(1, "offer", transition), null);
		QuestGraphItemActionAdapter items = new QuestGraphItemActionAdapter(7, new Object(), ignored -> 1,
			values -> true, (itemId, delta) -> false, (itemId, delta) -> false, () -> true, itemId -> true);
		AtomicInteger writes = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED,
			invocation -> READY, invocation -> APPLIED, items, (expected, state) -> {
				writes.incrementAndGet();
				return PersistenceResult.APPLIED;
			});

		assertEquals(DispatchResult.Status.REJECTED, executor.execute(match, context));
		assertEquals(0, writes.get());
		assertNull(states.get(1));
	}

	/**
	 * 验证库存不足的 optional exact 冻结为 no-op，并仍按 journal/CAS 合同提交任务状态。
	 * Verifies that an insufficient optional exact removal freezes as a no-op and still commits quest state through journal/CAS.
	 */
	@Test
	void insufficientOptionalExactRemoveStillCommitsState() {
		RemoveQuestItemAction remove = new RemoveQuestItemAction(182200001, 2,
			CompiledQuestGraph.QuestItemRemovalMode.OPTIONAL_EXACT);
		Transition transition = new Transition("remove", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"),
			List.of(new QuestStatusCondition(NONE)), List.of(new StartQuestAction(), remove));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", Map.of(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		Match match = new Match(EVENT, graph, new EventRoute(1, "offer", transition), null);
		AtomicInteger removals = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter items = new QuestGraphItemActionAdapter(7, new Object(), ignored -> 1,
			values -> true, (itemId, delta) -> false, (itemId, delta) -> {
				removals.incrementAndGet();
				return true;
			}, () -> {
				stores.incrementAndGet();
				return true;
			}, itemId -> true);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED,
			invocation -> READY, invocation -> APPLIED, items, cas(database));

		assertEquals(DispatchResult.Status.APPLIED, executor.execute(match, context));
		assertEquals("done", states.get(1).getNodeId());
		assertEquals(CompiledQuestGraph.QuestStatus.START, states.get(1).getQuestStatus());
		assertEquals(Lifecycle.ACTIVE, states.get(1).getLifecycle());
		assertEquals(0, removals.get());
		assertEquals(0, stores.get());
		assertEquals(states.get(1), database.get());
	}

	/**
	 * 验证直接完成、完成次数 set/add 与计时器启动共享同一 journal/CAS 执行合同。
	 * Verifies direct completion, completion-count set/add, and timer start share one journal/CAS execution contract.
	 */
	@Test
	void completionCountAndTimerStartArePersistedBeforeProtocol() {
		List<Action> actions = List.of(new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.COMPLETE),
			new SetCompletionCountAction(1), new AddCompletionCountAction(1), new StartQuestTimerAction("QUEST_TIMER", 300),
			new SyncQuestTimerAction("QUEST_TIMER", 300));
		Transition transition = new Transition("complete", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(), actions);
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicReference<QuestGraphTimerActionAdapter.StartCommand> start = new AtomicReference<>();
		AtomicReference<QuestGraphTimerActionAdapter.SyncCommand> sync = new AtomicReference<>();
		QuestGraphTimerActionAdapter timers = new QuestGraphTimerActionAdapter(command -> {
			start.set(command);
			return APPLIED;
		}, command -> FAILED, command -> {
			sync.set(command);
			return APPLIED;
		});
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> FAILED, noItemActions(), timers, cas(database));

		assertEquals(DispatchResult.Status.APPLIED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		PlayerQuestGraphState completed = states.get(1);
		assertEquals(CompiledQuestGraph.QuestStatus.COMPLETE, completed.getQuestStatus());
		assertEquals(2, completed.getHistory().completionCount());
		assertEquals(301_000L, completed.getDeadlines().get("QUEST_TIMER"));
		assertEquals(301_000L, start.get().deadlineAt());
		assertEquals(300, sync.get().remainingSeconds());
		assertEquals(completed, database.get());
	}

	/** 验证影片 typed adapter 只在 canonical 状态提交后接收稳定协议投影。 / Verifies the movie typed adapter receives a stable protocol projection only after canonical state commit. */
	@Test
	void movieProjectionRunsThroughTypedAdapterAfterCommit() {
		Transition transition = new Transition("movie", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(),
			List.of(new PlayMovieAction(913)));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicReference<QuestGraphMovieActionAdapter.PlayMovieCommand> movie = new AtomicReference<>();
		QuestGraphMovieActionAdapter movies = new QuestGraphMovieActionAdapter(7, command -> {
			assertEquals("done", states.get(1).getNodeId());
			assertEquals(Lifecycle.PREPARED, states.get(1).getLifecycle());
			assertTrue(states.get(1).getJournal().isTargetCommitted());
			movie.set(command);
			return APPLIED;
		}, command -> FAILED);
		QuestGraphTimerActionAdapter timers = new QuestGraphTimerActionAdapter(command -> FAILED, command -> FAILED, command -> FAILED);
		QuestGraphItemUseAnimationAdapter itemUseAnimations = new QuestGraphItemUseAnimationAdapter(7, command -> FAILED,
			(command, finish) -> new ScheduleResult(FAILED, null), command -> FAILED);
		QuestGraphDialogProtocolAdapter dialogs = new QuestGraphDialogProtocolAdapter(7, command -> FAILED);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> FAILED, noItemActions(), timers, movies, itemUseAnimations, dialogs, new QuestGraphTeleportActionAdapter(7, command -> FAILED),
			noInstanceSpawns(), cas(database));

		assertEquals(DispatchResult.Status.APPLIED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertEquals(913, movie.get().movieId());
		assertEquals("8:dialog-1:1:movie:7:0", movie.get().idempotencyKey());
		assertEquals(states.get(1), database.get());
	}

	/** 验证 UNBOUND 对话协议在状态提交后经 typed adapter 投影。 / Verifies UNBOUND dialog protocol projects through the typed adapter after state commit. */
	@Test
	void unboundDialogProjectionRunsThroughTypedAdapterAfterCommit() {
		Transition transition = new Transition("dialog", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(),
			List.of(new SendDialogAction(1352, CompiledQuestGraph.DialogBindingMode.UNBOUND)));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicReference<QuestGraphDialogProtocolAdapter.DialogCommand> dialog = new AtomicReference<>();
		QuestGraphDialogProtocolAdapter dialogs = new QuestGraphDialogProtocolAdapter(7, command -> {
			assertEquals("done", states.get(1).getNodeId());
			dialog.set(command);
			return APPLIED;
		});
		QuestGraphTimerActionAdapter timers = new QuestGraphTimerActionAdapter(command -> FAILED, command -> FAILED, command -> FAILED);
		QuestGraphMovieActionAdapter movies = new QuestGraphMovieActionAdapter(7, command -> FAILED, command -> FAILED);
		QuestGraphItemUseAnimationAdapter itemUseAnimations = new QuestGraphItemUseAnimationAdapter(7, command -> FAILED,
			(command, finish) -> new ScheduleResult(FAILED, null), command -> FAILED);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> FAILED, noItemActions(), timers, movies, itemUseAnimations, dialogs, new QuestGraphTeleportActionAdapter(7, command -> FAILED),
			noInstanceSpawns(), cas(database));

		assertEquals(DispatchResult.Status.APPLIED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertEquals(CompiledQuestGraph.DialogBindingMode.UNBOUND, dialog.get().binding());
		assertEquals(1352, dialog.get().dialogId());
		assertEquals(states.get(1), database.get());
	}

	/** 验证 REQUIRED spawn/teleport 由 typed adapter 同时拥有预检和执行。 / Verifies typed adapters own preflight and execution for required spawn/teleport actions. */
	@Test
	void spawnAndTeleportRequiredActionsUseTypedPreflightAndExecution() {
		AtomicInteger spawns = new AtomicInteger();
		AtomicInteger teleports = new AtomicInteger();
		QuestGraphInstanceSpawnAdapter instanceSpawns = new QuestGraphInstanceSpawnAdapter(7,
			query -> new QuestGraphInstanceSpawnAdapter.SpawnSpot(210010000, 0, 1, 2, 3, (byte) 0), command -> {
				spawns.incrementAndGet();
				return new QuestGraphInstanceSpawnAdapter.SpawnResult(APPLIED, 990001);
			}, command -> APPLIED);
		QuestGraphTeleportActionAdapter teleportsAdapter = new QuestGraphTeleportActionAdapter(7, command -> {
			teleports.incrementAndGet();
			return APPLIED;
		});
		QuestGraphTimerActionAdapter timers = new QuestGraphTimerActionAdapter(command -> FAILED, command -> FAILED, command -> FAILED);
		QuestGraphMovieActionAdapter movies = new QuestGraphMovieActionAdapter(7, command -> FAILED, command -> FAILED);
		QuestGraphItemUseAnimationAdapter itemUseAnimations = new QuestGraphItemUseAnimationAdapter(7, command -> FAILED,
			(command, finish) -> new ScheduleResult(FAILED, null), command -> FAILED);
		QuestGraphDialogProtocolAdapter dialogs = new QuestGraphDialogProtocolAdapter(7, command -> FAILED);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, new PlayerQuestGraphStateList(),
			invocation -> MATCHED, invocation -> QuestGraphTransitionExecutor.PreflightResult.FAILED, invocation -> FAILED, noItemActions(), timers, movies,
			itemUseAnimations, dialogs, teleportsAdapter, instanceSpawns, (expected, state) -> PersistenceResult.APPLIED);
		ActionInvocation spawn = new ActionInvocation(new SpawnInstanceNpcAction(700759, 216608), 1, 0,
			CompiledQuestGraph.QuestStatus.START, EVENT, RepeatDeadlineResolution.NOT_APPLICABLE, null, "spawn");
		ActionInvocation teleport = new ActionInvocation(new TeleportPlayerAction(210010000, 0, 1, 2, 3, (byte) 0), 1, 0,
			CompiledQuestGraph.QuestStatus.START, EVENT, RepeatDeadlineResolution.NOT_APPLICABLE, null, "teleport");

		assertEquals(READY, context.actionPreflight().apply(spawn));
		assertEquals(APPLIED, context.actionExecutor().apply(spawn));
		assertEquals(READY, context.actionPreflight().apply(teleport));
		assertEquals(APPLIED, context.actionExecutor().apply(teleport));
		assertEquals(1, spawns.get());
		assertEquals(1, teleports.get());
	}

	/** 验证 escort 作为 REQUIRED 原子动作执行，并在 canonical 状态中登记 QUEST_ESCORT cleanup lease。 / Verifies escort executes atomically as REQUIRED and registers a QUEST_ESCORT cleanup lease in canonical state. */
	@Test
	void escortRequiredActionUsesTypedAdapterAndPersistsCleanupLease() {
		StartEscortAction action = new StartEscortAction(EscortSource.EVENT_NPC, 0, (byte) 0, "4212", true, false,
			true, false, new EscortCoordinatesDestination(505.69427f, 437.69382f, 885.1844f));
		Transition transition = new Transition("escort", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(), List.of(action));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicReference<PlayerQuestGraphState> preparedSnapshot = new AtomicReference<>();
		AtomicInteger writes = new AtomicInteger();
		AtomicInteger preflights = new AtomicInteger();
		AtomicInteger starts = new AtomicInteger();
		QuestGraphEscortActionAdapter escorts = new QuestGraphEscortActionAdapter(7,
			() -> new QuestGraphEscortActionAdapter.PlayerContext(7, 210010000, 42, 501.5f, 438.25f, 885.0f), command -> {
				preflights.incrementAndGet();
				return READY;
			}, command -> {
				starts.incrementAndGet();
				return new QuestGraphEscortActionAdapter.StartResult(APPLIED, 990003, false, null);
			}, command -> APPLIED);
		QuestGraphTimerActionAdapter timers = new QuestGraphTimerActionAdapter(command -> FAILED, command -> FAILED, command -> FAILED);
		QuestGraphMovieActionAdapter movies = new QuestGraphMovieActionAdapter(7, command -> FAILED, command -> FAILED);
		QuestGraphItemUseAnimationAdapter animations = new QuestGraphItemUseAnimationAdapter(7, command -> FAILED,
			(command, finish) -> new ScheduleResult(FAILED, null), command -> FAILED);
		QuestGraphDialogProtocolAdapter dialogs = new QuestGraphDialogProtocolAdapter(7, command -> FAILED);
		QuestGraphLifecycleActionAdapter lifecycle = new QuestGraphLifecycleActionAdapter(7, command -> REJECTED, command -> FAILED);
		BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence = cas(database);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> REJECTED,
			invocation -> FAILED, noItemActions(), timers, movies, animations, dialogs, new QuestGraphTeleportActionAdapter(7, command -> FAILED),
			noInstanceSpawns(), escorts, lifecycle, (expected, state) -> {
				if (state.getLifecycle() == Lifecycle.PREPARED && state.getJournal().getNextActionIndex() == 0) {
					preparedSnapshot.set(state);
				}
				return writes.incrementAndGet() == 2 ? CONFLICT : persistence.apply(expected, state);
			});

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertEquals(Lifecycle.PREPARED, states.get(1).getLifecycle());
		assertEquals(DispatchResult.Status.APPLIED, executor.recover(graph, context));
		assertEquals(1, preflights.get());
		assertEquals(1, starts.get());
		assertEquals("done", states.get(1).getNodeId());
		String idempotencyKey = "8:dialog-1:1:escort:7:0";
		CleanupLease lease = states.get(1).getCleanupLeases().get(idempotencyKey);
		assertTrue(lease.resolved());
		assertEquals("QUEST_ESCORT", lease.capability());
		assertEquals(idempotencyKey, lease.resourceKey());
		EscortResourceIdentity identity = (EscortResourceIdentity) lease.identity();
		EscortResourceIdentity plan = (EscortResourceIdentity) preparedSnapshot.get().getCleanupLeases()
			.get(idempotencyKey).identity();
		assertEquals(0, plan.objectId());
		assertEquals(210010000, plan.worldId());
		assertEquals(42, plan.instanceId());
		assertEquals(900100, plan.eventNpcObjectId());
		assertEquals(7, identity.playerId());
		assertEquals(1, identity.questId());
		assertEquals(990003, identity.objectId());
		assertEquals(100, identity.npcId());
		assertEquals(210010000, identity.worldId());
		assertEquals(42, identity.instanceId());
		assertEquals(100, identity.eventNpcId());
		assertEquals(900100, identity.eventNpcObjectId());
		assertEquals(action, identity.action());
		assertEquals(idempotencyKey, identity.idempotencyKey());
		assertFalse(identity.spawnedFollower());
		assertEquals(states.get(1), database.get());
	}

	@Test
	void startQuestPreservesFrozenResourcePlanAcrossRecovery() {
		SpawnInstanceNpcAction spawn = new SpawnInstanceNpcAction(700759, 216608);
		Transition transition = new Transition("start-and-spawn", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(),
			List.of(new StartQuestAction(), spawn));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", Map.of(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		AtomicInteger preparations = new AtomicInteger();
		AtomicInteger spawnAttempts = new AtomicInteger();
		AtomicReference<InstanceSpawnResourceIdentity> frozen = new AtomicReference<>();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY, invocation -> {
			if (invocation.action() instanceof SpawnInstanceNpcAction) {
				return spawnAttempts.incrementAndGet() == 1 ? FAILED : APPLIED;
			}
			return APPLIED;
		}, ignored -> 0, ignored -> READY, invocation -> {
			preparations.incrementAndGet();
			InstanceSpawnResourceIdentity plan = new InstanceSpawnResourceIdentity(7, 1, 0, 216608, SpawnPlacementKind.FIXED,
				0, 0, 210010000, 1, 10, 20, 30, (byte) 0, invocation.idempotencyKey());
			frozen.set(plan);
			return CleanupLease.instanceSpawn(plan);
		}, invocation -> {
			CleanupLease persisted = invocation.cleanupLeases().get(invocation.idempotencyKey());
			assertEquals(frozen.get(), persisted.identity());
			return CleanupLease.instanceSpawn(frozen.get().materialize(990001));
		}, cas(database));
		Match match = new Match(EVENT, graph, new EventRoute(1, "offer", transition), null);

		assertEquals(DispatchResult.Status.FAILED, executor.execute(match, context));
		PlayerQuestGraphState interrupted = states.get(1);
		assertEquals(Lifecycle.PREPARED, interrupted.getLifecycle());
		assertEquals(1, interrupted.getJournal().getNextActionIndex());
		assertEquals(frozen.get(), interrupted.getCleanupLeases().values().iterator().next().identity());

		assertEquals(DispatchResult.Status.APPLIED, executor.recover(graph, context));
		assertEquals(1, preparations.get());
		assertEquals(2, spawnAttempts.get());
		assertTrue(states.get(1).getCleanupLeases().values().iterator().next().identity().materialized());
		assertEquals(Lifecycle.ACTIVE, states.get(1).getLifecycle());
	}

	/** 验证原有组合构造器保持源码兼容，且新增动作仍由旧调用方的通用端口处理。 / Verifies legacy composite constructors remain source-compatible and route added actions through the caller's generic ports. */
	@Test
	void legacyCompositeConstructorsRemainCompatibleWithoutNullAdapters() throws NoSuchMethodException {
		Class<?>[] prefix = { int.class, int.class, ZoneId.class, PlayerQuestGraphStateList.class,
			Function.class, Function.class, Function.class, QuestGraphItemActionAdapter.class,
			QuestGraphTimerActionAdapter.class, QuestGraphMovieActionAdapter.class };
		TransitionContext.class.getConstructor(concat(prefix, QuestGraphLifecycleActionAdapter.class,
			QuestGraphRecipeBridge.class, BiFunction.class));
		TransitionContext.class.getConstructor(concat(prefix, QuestGraphLifecycleActionAdapter.class,
			QuestGraphRecipeBridge.class, QuestGraphCraftSkillRewardBridge.class, BiFunction.class));
		TransitionContext.class.getConstructor(concat(prefix, QuestGraphLifecycleActionAdapter.class, BiFunction.class));
		TransitionContext.class.getConstructor(concat(prefix, BiFunction.class));

		AtomicInteger preflights = new AtomicInteger();
		AtomicInteger executions = new AtomicInteger();
		QuestGraphTimerActionAdapter timers = new QuestGraphTimerActionAdapter(command -> FAILED, command -> FAILED, command -> FAILED);
		QuestGraphMovieActionAdapter movies = new QuestGraphMovieActionAdapter(7, command -> FAILED, command -> FAILED);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, new PlayerQuestGraphStateList(), invocation -> MATCHED,
			invocation -> {
				preflights.incrementAndGet();
				return READY;
			}, invocation -> {
				executions.incrementAndGet();
				return APPLIED;
			}, noItemActions(), timers, movies, (expected, state) -> PersistenceResult.APPLIED);
		ActionInvocation teleport = new ActionInvocation(new TeleportPlayerAction(210010000, 0, 1, 2, 3, (byte) 0), 1, 0,
			CompiledQuestGraph.QuestStatus.START, EVENT, RepeatDeadlineResolution.NOT_APPLICABLE, null, "legacy-teleport");

		assertEquals(READY, context.actionPreflight().apply(teleport));
		assertEquals(APPLIED, context.actionExecutor().apply(teleport));
		assertEquals(1, preflights.get());
		assertEquals(1, executions.get());
	}

	/**
	 * 验证制作 bridge 独占资格、required 预检/执行和提交后协议路径。
	 * Verifies the craft bridge exclusively owns eligibility, required preflight/execution, and post-commit protocol paths.
	 */
	@Test
	void craftBridgeRoutesConditionRequiredGrantAndPostCommitProtocol() {
		CraftFixture fixture = craftFixture();
		AtomicInteger delegatedConditions = new AtomicInteger();
		AtomicInteger delegatedPreflights = new AtomicInteger();
		AtomicInteger delegatedActions = new AtomicInteger();
		AtomicInteger eligibility = new AtomicInteger();
		AtomicInteger preflights = new AtomicInteger();
		AtomicInteger grants = new AtomicInteger();
		AtomicInteger protocols = new AtomicInteger();
		QuestGraphCraftSkillRewardBridge bridge = new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CRAFT_REFERENCES, query -> {
			eligibility.incrementAndGet();
			return new QuestGraphCraftSkillRewardBridge.EligibilitySnapshot(true, 399, true);
		}, command -> {
			preflights.incrementAndGet();
			return READY;
		}, command -> {
			grants.incrementAndGet();
			return APPLIED;
		}, command -> {
			protocols.incrementAndGet();
			assertEquals(Lifecycle.PREPARED, fixture.states().get(1941).getLifecycle());
			assertEquals("reward", fixture.states().get(1941).getNodeId());
			assertTrue(fixture.states().get(1941).getJournal().isTargetCommitted());
			return APPLIED;
		}, command -> FAILED);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(), invocation -> {
			delegatedConditions.incrementAndGet();
			return QuestGraphTransitionExecutor.ConditionResult.FAILED;
		}, invocation -> {
			delegatedPreflights.incrementAndGet();
			return QuestGraphTransitionExecutor.PreflightResult.FAILED;
		}, invocation -> {
			delegatedActions.incrementAndGet();
			return FAILED;
		}, bridge, cas(fixture.database()));

		assertEquals(DispatchResult.Status.APPLIED, executor.execute(fixture.match(), context));
		assertEquals(1, eligibility.get());
		assertEquals(1, preflights.get());
		assertEquals(1, grants.get());
		assertEquals(1, protocols.get());
		assertEquals(0, delegatedConditions.get());
		assertEquals(0, delegatedPreflights.get());
		assertEquals(0, delegatedActions.get());
		assertEquals(5, fixture.states().get(1941).getRevision());
		assertEquals(fixture.states().get(1941), fixture.database().get());
	}

	/**
	 * 验证 grant 失败保留 PREPARED journal，恢复复用稳定命令且协议只在提交后执行。
	 * Verifies grant failure retains PREPARED, recovery reuses a stable command, and protocol runs only after commit.
	 */
	@Test
	void craftBridgeRecoveryReusesStableGrantIdentityAndDefersProtocol() {
		CraftFixture fixture = craftFixture();
		AtomicReference<QuestGraphCraftSkillRewardBridge.GrantCommand> failedGrant = new AtomicReference<>();
		AtomicInteger prematureProtocols = new AtomicInteger();
		QuestGraphCraftSkillRewardBridge failing = new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CRAFT_REFERENCES,
			query -> new QuestGraphCraftSkillRewardBridge.EligibilitySnapshot(false, 0, true), command -> READY, command -> {
				failedGrant.set(command);
				return FAILED;
			}, command -> {
				prematureProtocols.incrementAndGet();
				return FAILED;
			}, command -> FAILED);
		TransitionContext first = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(),
			invocation -> QuestGraphTransitionExecutor.ConditionResult.FAILED,
			invocation -> QuestGraphTransitionExecutor.PreflightResult.FAILED, invocation -> FAILED, failing, cas(fixture.database()));

		assertEquals(DispatchResult.Status.FAILED, executor.execute(fixture.match(), first));
		assertEquals(Lifecycle.PREPARED, fixture.states().get(1941).getLifecycle());
		assertEquals(0, fixture.states().get(1941).getJournal().getNextActionIndex());
		assertEquals(0, prematureProtocols.get());
		assertFalse(failedGrant.get().recipeIds().isEmpty());
		assertEquals(failedGrant.get().recipeIds().stream().sorted().toList(), failedGrant.get().recipeIds());

		AtomicReference<QuestGraphCraftSkillRewardBridge.GrantCommand> recoveredGrant = new AtomicReference<>();
		AtomicInteger protocols = new AtomicInteger();
		QuestGraphCraftSkillRewardBridge recovering = new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CRAFT_REFERENCES,
			query -> new QuestGraphCraftSkillRewardBridge.EligibilitySnapshot(false, 0, true), command -> READY, command -> {
				recoveredGrant.set(command);
				return ALREADY_APPLIED;
			}, command -> {
				protocols.incrementAndGet();
				assertEquals(Lifecycle.PREPARED, fixture.states().get(1941).getLifecycle());
				assertTrue(fixture.states().get(1941).getJournal().isTargetCommitted());
				return APPLIED;
			}, command -> FAILED);
		TransitionContext recovery = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(),
			invocation -> QuestGraphTransitionExecutor.ConditionResult.FAILED,
			invocation -> QuestGraphTransitionExecutor.PreflightResult.FAILED, invocation -> FAILED, recovering, cas(fixture.database()));

		assertEquals(DispatchResult.Status.APPLIED, executor.recover(fixture.graph(), recovery));
		assertEquals(failedGrant.get(), recoveredGrant.get());
		assertEquals(1, protocols.get());
		assertEquals(Lifecycle.ACTIVE, fixture.states().get(1941).getLifecycle());
		assertNull(fixture.states().get(1941).getJournal());
	}

	/**
	 * 验证制作 grant 预检拒绝时不会写入 PREPARED，也不会执行 grant 或提交后协议。
	 * Verifies a rejected craft-grant preflight writes no PREPARED state and executes neither grant nor post-commit protocol.
	 */
	@Test
	void craftGrantPreflightRejectionLeavesCanonicalStateUntouched() {
		CraftFixture fixture = craftFixture();
		AtomicInteger grants = new AtomicInteger();
		AtomicInteger protocols = new AtomicInteger();
		QuestGraphCraftSkillRewardBridge bridge = new QuestGraphCraftSkillRewardBridge(7, Race.ELYOS, CRAFT_REFERENCES,
			query -> new QuestGraphCraftSkillRewardBridge.EligibilitySnapshot(true, 399, true), command -> REJECTED, command -> {
				grants.incrementAndGet();
				return APPLIED;
			}, command -> {
				protocols.incrementAndGet();
				return APPLIED;
			}, command -> FAILED);
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, fixture.states(),
			invocation -> QuestGraphTransitionExecutor.ConditionResult.FAILED,
			invocation -> QuestGraphTransitionExecutor.PreflightResult.FAILED, invocation -> FAILED, bridge, cas(fixture.database()));

		assertEquals(DispatchResult.Status.REJECTED, executor.execute(fixture.match(), context));
		assertEquals(0, grants.get());
		assertEquals(0, protocols.get());
		assertEquals(Lifecycle.ACTIVE, fixture.states().get(1941).getLifecycle());
		assertNull(fixture.states().get(1941).getJournal());
		assertEquals(fixture.database().get(), fixture.states().get(1941));
	}

	/**
	 * 验证 COMPLETE 与零完成次数的非法组合在 PREPARED 写入前失败。
	 * Verifies an invalid COMPLETE/zero-count combination fails before the PREPARED write.
	 */
	@Test
	void invalidCompletionHistoryFailsBeforeAnyWrite() {
		List<Action> actions = List.of(new SetQuestStatusAction(CompiledQuestGraph.QuestStatus.COMPLETE), new SetCompletionCountAction(0));
		Transition transition = new Transition("invalid", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(), actions);
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicInteger writes = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> APPLIED, (expected, state) -> {
				writes.incrementAndGet();
				return PersistenceResult.APPLIED;
			});

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertEquals(0, writes.get());
		assertEquals(initial, states.get(1));
	}

	/**
	 * 验证计时器停止通过 typed bridge 后删除持久化 deadline，并在提交后同步零秒。
	 * Verifies timer end removes the persisted deadline through the typed bridge and syncs zero after commit.
	 */
	@Test
	void timerEndRemovesDeadlineAndSyncsAfterCommit() {
		List<Action> actions = List.of(new EndQuestTimerAction("QUEST_TIMER"), new SyncQuestTimerAction("QUEST_TIMER", 0));
		Transition transition = new Transition("stop", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(), actions);
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1, 1, 0, "active", CompiledQuestGraph.QuestStatus.START,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of("QUEST_TIMER", 500_000L), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicReference<QuestGraphTimerActionAdapter.EndCommand> end = new AtomicReference<>();
		AtomicReference<QuestGraphTimerActionAdapter.SyncCommand> sync = new AtomicReference<>();
		QuestGraphTimerActionAdapter timers = new QuestGraphTimerActionAdapter(command -> FAILED, command -> {
			end.set(command);
			return APPLIED;
		}, command -> {
			sync.set(command);
			return APPLIED;
		});
		TransitionContext context = new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY,
			invocation -> FAILED, noItemActions(), timers, cas(database));

		assertEquals(DispatchResult.Status.APPLIED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertEquals(Map.of(), states.get(1).getDeadlines());
		assertEquals("QUEST_TIMER", end.get().timer());
		assertEquals(0, sync.get().remainingSeconds());
	}

	/**
	 * 创建聚焦测试使用的执行上下文。
	 * Creates the execution context used by focused tests.
	 */
	private static TransitionContext context(PlayerQuestGraphStateList states, AtomicReference<PlayerQuestGraphState> database,
		java.util.function.Function<ActionInvocation, QuestGraphTransitionExecutor.ActionResult> action) {
		return new TransitionContext(7, 0, SERVER_ZONE, states, invocation -> MATCHED, invocation -> READY, action, cas(database));
	}

	/**
	 * 创建严格按 revision 更新的内存 CAS。
	 * Creates an in-memory CAS that strictly advances by revision.
	 */
	private static BiFunction<Long, PlayerQuestGraphState, PersistenceResult> cas(AtomicReference<PlayerQuestGraphState> database) {
		return (expected, next) -> {
			PlayerQuestGraphState current = database.get();
			if (expected == null ? current != null : current == null || current.getRevision() != expected) {
				return CONFLICT;
			}
			database.set(next);
			return PersistenceResult.APPLIED;
		};
	}

	/** 创建不处理任何物品动作的聚焦 adapter。 / Creates a focused adapter that handles no item actions. */
	private static QuestGraphItemActionAdapter noItemActions() {
		return new QuestGraphItemActionAdapter(7, new Object(), ignored -> 0, values -> true, (itemId, delta) -> false,
			(itemId, delta) -> false, () -> true, itemId -> true);
	}

	/** 拼接反射参数表。 / Concatenates reflected constructor parameter lists. */
	private static Class<?>[] concat(Class<?>[] prefix, Class<?>... suffix) {
		Class<?>[] result = new Class<?>[prefix.length + suffix.length];
		System.arraycopy(prefix, 0, result, 0, prefix.length);
		System.arraycopy(suffix, 0, result, prefix.length, suffix.length);
		return result;
	}

	/** 创建拒绝所有 instance spawn 的测试 adapter。 / Creates a test adapter that rejects every instance spawn. */
	private static QuestGraphInstanceSpawnAdapter noInstanceSpawns() {
		return new QuestGraphInstanceSpawnAdapter(7,
			query -> new QuestGraphInstanceSpawnAdapter.SpawnSpot(210010000, 0, 0, 0, 0, (byte) 0),
			command -> new QuestGraphInstanceSpawnAdapter.SpawnResult(FAILED, 0), command -> FAILED);
	}

	/** 从正式 RecipeData 构造制作奖励测试共享的独立引用目录。 / Builds the independent formal-RecipeData reference catalog shared by craft-reward tests. */
	private static QuestGraphCraftSkillReferenceCatalog craftReferences() {
		try {
			RecipeData recipes = (RecipeData) JAXBContext.newInstance(RecipeData.class).createUnmarshaller()
				.unmarshal(new File("src/main/resources/aion/data/static_data/recipe/recipe_templates.xml"));
			return QuestGraphCraftSkillReferenceCatalog.build(recipes);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load formal craft references", e);
		}
	}

	/** 创建制作奖励 required/protocol 自转换 fixture。 / Creates a craft-reward required/protocol self-transition fixture. */
	private static CraftFixture craftFixture() {
		MovieEndedEvent event = new MovieEndedEvent("craft-movie", 7, 2000, 93, 1, 1900);
		Transition transition = new Transition("grant-craft", 10, "reward", new Event(MOVIE_ENDED, 93, null),
			List.of(new CraftSkillEligibilityCondition(40002, 400, CraftSkillEligibilityPolicy.CAPACITY_REQUIRED)),
			List.of(new GrantCraftSkillRewardAction(40002, 400), new SyncCraftSkillRewardAction(40002)));
		CompiledQuestGraph graph = new CompiledQuestGraph(1941, 1, PLAYER, "reward", Map.of(),
			Map.of("reward", new Node("reward", false, List.of(transition))));
		PlayerQuestGraphState initial = new PlayerQuestGraphState(1941, 1, 0, "reward", CompiledQuestGraph.QuestStatus.REWARD,
			QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		return new CraftFixture(graph, states, database, new Match(event, graph, new EventRoute(1941, "reward", transition), initial));
	}

	/**
	 * 创建包含一个条件、一个动作和一个终态的最小图 fixture。
	 * Creates a minimal graph fixture with one condition, one action, and one terminal node.
	 */
	private static Fixture fixture() {
		return fixture(List.of(new QuestStatusCondition(NONE), new PlayerLevelCondition(1, null)));
	}

	/**
	 * 创建使用指定条件列表的最小图 fixture。
	 * Creates a minimal graph fixture using the specified condition list.
	 */
	private static Fixture fixture(List<Condition> conditions) {
		List<Action> actions = List.of(new StartQuestAction(), new RemoveCollectedItemsAction());
		Transition transition = new Transition("accept", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"),
			conditions, actions);
		Map<String, CompiledQuestGraph.Variable> variables = new LinkedHashMap<>();
		variables.put("count", new IntVariable("count", PLAYER, 2, 0, 5));
		variables.put("enabled", new BooleanVariable("enabled", PLAYER, false));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", variables,
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		return new Fixture(graph, states, new Match(EVENT, graph, new EventRoute(1, "offer", transition), null));
	}

	/** 创建指向 fixture 首个转换的初始 PREPARED 状态。 / Creates an initial PREPARED state targeting the fixture's first transition. */
	private static PlayerQuestGraphState preparedState(CompiledQuestGraph graph) {
		PreparedTransition journal = new PreparedTransition(-1, EVENT.eventId(), "accept", 0, QuestGraphEventCodec.encode(EVENT));
		return new PlayerQuestGraphState(1, 1, 0, graph.initialNode(), NONE, QuestHistory.EMPTY, null, Lifecycle.PREPARED,
			Map.of("count", new IntValue(2), "enabled", new BooleanValue(false)), Map.of(), journal, Map.of(), null);
	}

	/**
	 * 保存单个转换测试共享的图、状态列表和路由匹配。
	 * Holds the graph, state list, and route match shared by one transition test.
	 */
	private record Fixture(CompiledQuestGraph graph, PlayerQuestGraphStateList states, Match match) {
	}

	/** 保存制作奖励恢复测试共享的图、状态、数据库和匹配。 / Holds graph, state, database, and match shared by craft-reward recovery tests. */
	private record CraftFixture(CompiledQuestGraph graph, PlayerQuestGraphStateList states,
			AtomicReference<PlayerQuestGraphState> database, Match match) {
	}
}
