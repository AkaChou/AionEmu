package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ActionType.START_QUEST;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.ConditionType.QUEST_STATUS;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.DIALOG;
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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.BooleanVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Condition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.IntVariable;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.BooleanValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.IntValue;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 验证任务图转换的预检、CAS、journal 推进和恢复重放。
 * Verifies quest-graph transition preflight, CAS, journal progress, and recovery replay.
 */
class QuestGraphTransitionExecutorTest {

	private static final DialogEvent EVENT = new DialogEvent("dialog-1", 7, 1000, 100, "QUEST_SELECT");
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
		assertEquals(2, state.getRevision());
		assertEquals("done", state.getNodeId());
		assertEquals(Lifecycle.ACTIVE, state.getLifecycle());
		assertEquals(CompiledQuestGraph.QuestStatus.START, state.getQuestStatus());
		assertNull(state.getJournal());
		assertEquals(new IntValue(2), state.getVariables().get("count"));
		assertEquals(new BooleanValue(false), state.getVariables().get("enabled"));
		assertEquals("8:dialog-1:1:accept:7:0", action.get().idempotencyKey());
		assertEquals(state, database.get());
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
		TransitionContext mismatchContext = new TransitionContext(7, mismatch.states(), invocation -> NOT_MATCHED, invocation -> READY,
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
		TransitionContext rejectedContext = new TransitionContext(7, rejected.states(), invocation -> MATCHED, invocation -> REJECTED,
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
		PlayerQuestGraphState active = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.START, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		fixture.states().addLoaded(active);
		AtomicInteger callbacks = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, fixture.states(), invocation -> {
			callbacks.incrementAndGet();
			return MATCHED;
		}, invocation -> READY, invocation -> APPLIED, (expected, state) -> PersistenceResult.APPLIED);
		Match match = new Match(EVENT, fixture.graph(), fixture.match().route(), active);

		assertEquals(DispatchResult.Status.NO_MATCH, executor.execute(match, context));
		assertEquals(0, callbacks.get());
		assertEquals(active, fixture.states().get(1));
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
		PlayerQuestGraphState state = new PlayerQuestGraphState(1, 1, 0, "offer", CompiledQuestGraph.QuestStatus.REWARD, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null);
		fixture.states().addLoaded(state);
		AtomicInteger callbacks = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, fixture.states(), invocation -> MATCHED, invocation -> {
			callbacks.incrementAndGet();
			return READY;
		}, invocation -> APPLIED, (expected, next) -> PersistenceResult.APPLIED);

		assertEquals(DispatchResult.Status.FAILED,
			executor.execute(new Match(EVENT, graph, new EventRoute(1, "offer", transition), state), context));
		assertEquals(0, callbacks.get());
		assertEquals(state, fixture.states().get(1));
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
	 * 验证准备状态 CAS 冲突会在任何动作执行前阻断转换。
	 * Verifies that a PREPARED-state CAS conflict blocks the transition before any action executes.
	 */
	@Test
	void prepareCasConflictBlocksActions() {
		Fixture fixture = fixture();
		AtomicInteger actions = new AtomicInteger();
		TransitionContext context = new TransitionContext(7, fixture.states(), invocation -> MATCHED, invocation -> READY, invocation -> {
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
		TransitionContext context = new TransitionContext(7, base.states(), invocation -> {
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
		TransitionContext context = new TransitionContext(7, fixture.states(), invocation -> {
			callbacks.incrementAndGet();
			return MATCHED;
		}, invocation -> READY, invocation -> APPLIED, (expected, state) -> PersistenceResult.APPLIED);

		assertEquals(DispatchResult.Status.FAILED, executor.execute(foreignMatch, context));
		assertEquals(0, callbacks.get());
		assertNull(fixture.states().get(1));
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
		TransitionContext context = new TransitionContext(7, fixture.states(), invocation -> MATCHED, invocation -> READY, invocation -> {
			calls.incrementAndGet();
			return effects.add(invocation.idempotencyKey()) ? APPLIED : ALREADY_APPLIED;
		}, (expected, state) -> writes.incrementAndGet() == 2 ? CONFLICT : cas.apply(expected, state));

		assertEquals(DispatchResult.Status.FAILED, executor.execute(fixture.match(), context));
		assertEquals(Lifecycle.PREPARED, fixture.states().get(1).getLifecycle());
		assertEquals(CompiledQuestGraph.QuestStatus.NONE, fixture.states().get(1).getQuestStatus());
		assertEquals(DispatchResult.Status.APPLIED, executor.recover(fixture.graph(), context));

		PlayerQuestGraphState committed = fixture.states().get(1);
		assertEquals(2, calls.get());
		assertEquals(1, effects.size());
		assertEquals(2, committed.getRevision());
		assertEquals("done", committed.getNodeId());
		assertEquals(Lifecycle.ACTIVE, committed.getLifecycle());
		assertEquals(CompiledQuestGraph.QuestStatus.START, committed.getQuestStatus());
		assertEquals(committed, database.get());
	}

	/**
	 * 创建聚焦测试使用的执行上下文。
	 * Creates the execution context used by focused tests.
	 */
	private static TransitionContext context(PlayerQuestGraphStateList states, AtomicReference<PlayerQuestGraphState> database,
		java.util.function.Function<ActionInvocation, QuestGraphTransitionExecutor.ActionResult> action) {
		return new TransitionContext(7, states, invocation -> MATCHED, invocation -> READY, action, cas(database));
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

	/**
	 * 创建包含一个条件、一个动作和一个终态的最小图 fixture。
	 * Creates a minimal graph fixture with one condition, one action, and one terminal node.
	 */
	private static Fixture fixture() {
		Condition condition = new Condition(QUEST_STATUS, NONE);
		Action action = new Action(START_QUEST);
		Transition transition = new Transition("accept", 10, "done", new Event(DIALOG, 100, "QUEST_SELECT"),
			List.of(condition), List.of(action));
		Map<String, CompiledQuestGraph.Variable> variables = new LinkedHashMap<>();
		variables.put("count", new IntVariable("count", PLAYER, 2, 0, 5));
		variables.put("enabled", new BooleanVariable("enabled", PLAYER, false));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, PLAYER, "offer", variables,
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		return new Fixture(graph, states, new Match(EVENT, graph, new EventRoute(1, "offer", transition), null));
	}

	/**
	 * 保存单个转换测试共享的图、状态列表和路由匹配。
	 * Holds the graph, state list, and route match shared by one transition test.
	 */
	private record Fixture(CompiledQuestGraph graph, PlayerQuestGraphStateList states, Match match) {
	}
}
