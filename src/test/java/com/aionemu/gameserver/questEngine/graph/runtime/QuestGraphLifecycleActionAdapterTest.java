package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType.DIALOG;
import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope.PLAYER;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult.MATCHED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult.CONFLICT;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.READY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.AbandonQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.FinishQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveCollectedItemsAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestWorkItemsAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEventQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartQuestAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.AbandonCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.CollectedItemsCleanupCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.EventStartCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.LifecycleCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.SettlementCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.StartCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphLifecycleActionAdapter.WorkItemsCleanupCommand;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 验证任务启动、活动刷新、结算和放弃共享同一 typed lifecycle/cleanup bridge。
 * Verifies quest start, event refresh, settlement, and abandonment share one typed lifecycle/cleanup bridge.
 */
class QuestGraphLifecycleActionAdapterTest {

	private static final DialogEvent EVENT = new DialogEvent("lifecycle-event", 7, 1000, 100, "QUEST_SELECT");
	private static final Map<String, CleanupLease> LEASES = Map.of("escort", new CleanupLease("SPAWN", "npc:9001"));

	/**
	 * 验证五类 lifecycle 动作产生封闭命令，错误 owner 和非法状态失败关闭。
	 * Verifies five lifecycle actions produce closed commands while wrong owners and invalid states fail closed.
	 */
	@Test
	void mapsClosedLifecycleCommandsAndRejectsInvalidAuthority() {
		List<LifecycleCommand> commands = new ArrayList<>();
		QuestGraphLifecycleActionAdapter adapter = new QuestGraphLifecycleActionAdapter(7, command -> {
			commands.add(command);
			return READY;
		}, command -> APPLIED);

		assertEquals(READY, adapter.preflight(invocation(new StartQuestAction(), QuestStatus.NONE, "start")));
		assertEquals(READY, adapter.preflight(invocation(new StartEventQuestAction(2, QuestStatus.START),
			QuestStatus.START, "event-start")));
		assertEquals(READY, adapter.preflight(invocation(new RemoveCollectedItemsAction(), QuestStatus.REWARD, "collect")));
		assertEquals(READY, adapter.preflight(invocation(new RemoveQuestWorkItemsAction(), QuestStatus.START, "work-items")));
		assertEquals(READY, adapter.preflight(invocation(new FinishQuestAction(3), QuestStatus.REWARD, "finish")));
		assertEquals(READY, adapter.preflight(invocation(new AbandonQuestAction(), QuestStatus.START, "abandon")));

		assertInstanceOf(StartCommand.class, commands.get(0));
		EventStartCommand eventStart = assertInstanceOf(EventStartCommand.class, commands.get(1));
		assertEquals(2, eventStart.targetQuestId());
		assertEquals(QuestStatus.START, eventStart.status());
		assertInstanceOf(CollectedItemsCleanupCommand.class, commands.get(2));
		assertInstanceOf(WorkItemsCleanupCommand.class, commands.get(3));
		SettlementCommand settlement = assertInstanceOf(SettlementCommand.class, commands.get(4));
		assertEquals(3, settlement.rewardIndex());
		assertEquals(LEASES, settlement.cleanupLeases());
		assertInstanceOf(AbandonCommand.class, commands.get(5));
		assertEquals(QuestGraphTransitionExecutor.PreflightResult.FAILED,
			adapter.preflight(invocation(new FinishQuestAction(0), QuestStatus.START, "bad-status")));
		assertEquals(QuestGraphTransitionExecutor.PreflightResult.FAILED,
			adapter.preflight(new ActionInvocation(new StartQuestAction(), 1, 0, QuestStatus.NONE,
				new DialogEvent("wrong-owner", 8, 1000, 100, "QUEST_SELECT"), RepeatDeadlineResolution.NOT_APPLICABLE, null, LEASES, "wrong")));
	}

	/**
	 * 验证当前 owner 的活动任务启动仅在 typed bridge 成功后重置状态、deadline 和 cleanup。
	 * Verifies current-owner event start resets status, deadlines, and cleanup only after typed bridge success.
	 */
	@Test
	void currentOwnerEventStartResetsCanonicalStateAfterBridgeSuccess() {
		Transition transition = new Transition("event-start", 1, "active", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(),
			List.of(new StartEventQuestAction(1, QuestStatus.START)));
		CompiledQuestGraph graph = graph(transition);
		PlayerQuestGraphState initial = state(QuestStatus.REWARD);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		QuestGraphLifecycleActionAdapter adapter = new QuestGraphLifecycleActionAdapter(7, ignored -> READY, ignored -> APPLIED);
		TransitionContext context = new TransitionContext(7, 0, ZoneId.of("Asia/Shanghai"), states, invocation -> MATCHED,
			invocation -> READY, invocation -> FAILED, adapter, cas(database));

		assertEquals(DispatchResult.Status.APPLIED, new QuestGraphTransitionExecutor().execute(
			new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertEquals(QuestStatus.START, states.get(1).getQuestStatus());
		assertEquals(Map.of(), states.get(1).getDeadlines());
		assertEquals(Map.of(), states.get(1).getCleanupLeases());
	}

	/**
	 * 验证结算失败保留 PREPARED 与 cleanup ledger，恢复使用同一幂等键并在提交时清空资源。
	 * Verifies settlement failure preserves PREPARED and cleanup ledger, while recovery reuses the key and clears resources on commit.
	 */
	@Test
	void settlementFailureRecoversAndClearsLifecycleResources() {
		Transition transition = new Transition("finish", 1, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(),
			List.of(new FinishQuestAction(2)));
		CompiledQuestGraph graph = graph(transition);
		PlayerQuestGraphState initial = state(QuestStatus.REWARD);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicInteger attempts = new AtomicInteger();
		List<String> keys = new ArrayList<>();
		QuestGraphLifecycleActionAdapter adapter = new QuestGraphLifecycleActionAdapter(7, command -> READY, command -> {
			keys.add(command.idempotencyKey());
			return attempts.getAndIncrement() == 0 ? FAILED : APPLIED;
		});
		TransitionContext context = new TransitionContext(7, 0, ZoneId.of("Asia/Shanghai"), states, invocation -> MATCHED,
			invocation -> READY, invocation -> FAILED, adapter, cas(database));
		Match match = new Match(EVENT, graph, new EventRoute(1, "active", transition), initial);

		assertEquals(DispatchResult.Status.FAILED, new QuestGraphTransitionExecutor().execute(match, context));
		assertEquals(Lifecycle.PREPARED, states.get(1).getLifecycle());
		assertEquals(LEASES, states.get(1).getCleanupLeases());
		assertEquals(DispatchResult.Status.APPLIED, new QuestGraphTransitionExecutor().recover(graph, context));
		assertEquals(List.of(keys.getFirst(), keys.getFirst()), keys);
		assertEquals(QuestStatus.COMPLETE, states.get(1).getQuestStatus());
		assertEquals(Map.of(), states.get(1).getCleanupLeases());
		assertEquals(Map.of(), states.get(1).getDeadlines());
		assertEquals(states.get(1), database.get());
	}

	/** 验证放弃仅在 typed cleanup 成功后重置当前任务状态。 / Verifies abandonment resets current state only after typed cleanup succeeds. */
	@Test
	void abandonmentClearsStateOnlyAfterTypedCleanup() {
		Transition transition = new Transition("abandon", 1, "done", new Event(DIALOG, 100, "QUEST_SELECT"), List.of(),
			List.of(new AbandonQuestAction()));
		CompiledQuestGraph graph = graph(transition);
		PlayerQuestGraphState initial = state(QuestStatus.START);
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(initial);
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>(initial);
		AtomicReference<LifecycleCommand> command = new AtomicReference<>();
		QuestGraphLifecycleActionAdapter adapter = new QuestGraphLifecycleActionAdapter(7, ignored -> READY, value -> {
			command.set(value);
			return APPLIED;
		});
		TransitionContext context = new TransitionContext(7, 0, ZoneId.of("Asia/Shanghai"), states, invocation -> MATCHED,
			invocation -> READY, invocation -> FAILED, adapter, cas(database));

		assertEquals(DispatchResult.Status.APPLIED, new QuestGraphTransitionExecutor().execute(
			new Match(EVENT, graph, new EventRoute(1, "active", transition), initial), context));
		assertInstanceOf(AbandonCommand.class, command.get());
		assertEquals(QuestStatus.NONE, states.get(1).getQuestStatus());
		assertEquals(Map.of(), states.get(1).getCleanupLeases());
		assertEquals(Map.of(), states.get(1).getDeadlines());
	}

	/** 创建聚焦 adapter 调用。 / Creates a focused adapter invocation. */
	private static ActionInvocation invocation(CompiledQuestGraph.Action action, QuestStatus status, String key) {
		return new ActionInvocation(action, 1, 0, status, EVENT, RepeatDeadlineResolution.NOT_APPLICABLE, null, LEASES, key);
	}

	/** 创建单转换玩家图。 / Creates a single-transition player graph. */
	private static CompiledQuestGraph graph(Transition transition) {
		return new CompiledQuestGraph(1, 1, PLAYER, "active", Map.of(), Map.of(
			"active", new Node("active", false, List.of(transition)), "done", new Node("done", true, List.of())));
	}

	/** 创建携带 deadline 与 cleanup lease 的稳定状态。 / Creates stable state with a deadline and cleanup lease. */
	private static PlayerQuestGraphState state(QuestStatus status) {
		return new PlayerQuestGraphState(1, 1, 0, "active", status, QuestHistory.EMPTY, null, Lifecycle.ACTIVE, Map.of(),
			Map.of("QUEST_TIMER", 5000L), null, LEASES, null);
	}

	/** 创建严格 revision CAS。 / Creates strict revision CAS. */
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
}
