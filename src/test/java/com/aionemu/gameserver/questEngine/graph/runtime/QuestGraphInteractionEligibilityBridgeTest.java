package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.InteractionAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayerLevelCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatusCondition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventKey;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.runtime.DispatchResult.Status;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.InteractionEligibilityEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInteractionEligibilityBridge.InteractionSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ConditionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.QuestHistory;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

class QuestGraphInteractionEligibilityBridgeTest {

	private static final int PLAYER_ID = 7;
	private static final int OBJECT_TEMPLATE_ID = 700196;
	private static final long NOW = 1_700_000_000_000L;

	/**
	 * 验证 APPLIED/STOP 是唯一允许结果，且只读查询不改变 revision、对象或 journal。
	 * Verifies APPLIED/STOP as the only allow result and that read-only queries do not change revision, object, or journal.
	 */
	@Test
	void allowsOnlyMatchedReadOnlySelfLoopWithoutStateMutation() {
		Fixture fixture = fixture();
		PlayerQuestGraphState before = fixture.states().get(1);
		InteractionSnapshot snapshot = snapshot(true);

		assertTrue(fixture.bridge().isAllowed("can-act", NOW, snapshot, fixture.states(), invocation -> ConditionResult.MATCHED));
		assertSame(before, fixture.states().get(1));
		assertEquals(4, fixture.states().get(1).getRevision());
		assertEquals(null, fixture.states().get(1).getJournal());

		assertFalse(fixture.bridge().isAllowed("not-matched", NOW + 1, snapshot, fixture.states(),
			invocation -> ConditionResult.NOT_MATCHED));
		assertFalse(fixture.bridge().isAllowed("failed", NOW + 2, snapshot, fixture.states(), invocation -> ConditionResult.FAILED));
	}

	/** 验证伪造 authority、错误目标和无匹配都 fail closed。 / Verifies forged authority, wrong targets, and no match all fail closed. */
	@Test
	void rejectsUnavailableOrUnmatchedInteraction() {
		Fixture fixture = fixture();
		assertFalse(fixture.bridge().isAllowed("unavailable", NOW, snapshot(false), fixture.states(),
			invocation -> ConditionResult.MATCHED));
		InteractionSnapshot wrongObject = new InteractionSnapshot(PLAYER_ID, OBJECT_TEMPLATE_ID + 1, 5001, 210010000, 1,
			InteractionAction.ACTION_ITEM_USE, true);
		assertFalse(fixture.bridge().isAllowed("wrong", NOW, wrongObject, fixture.states(), invocation -> ConditionResult.MATCHED));
		assertFalse(fixture.bridge().isAllowed("missing-states", NOW, snapshot(true), new PlayerQuestGraphStateList(),
			invocation -> ConditionResult.MATCHED));
	}

	/** 验证普通转换执行器拒绝 eligibility，绝不写 PREPARED。 / Verifies the normal transition executor rejects eligibility and never writes PREPARED. */
	@Test
	void normalTransitionExecutionCannotMutateThroughQueryEvent() {
		Fixture fixture = fixture();
		InteractionEligibilityEvent event = QuestGraphInteractionEligibilityBridge.event("execute", NOW, snapshot(true));
		AtomicInteger persistenceCalls = new AtomicInteger();
		TransitionContext context = new TransitionContext(PLAYER_ID, 0, ZoneId.of("UTC"), fixture.states(),
			invocation -> ConditionResult.MATCHED, invocation -> PreflightResult.READY,
			invocation -> QuestGraphTransitionExecutor.ActionResult.APPLIED, (expected, state) -> {
				persistenceCalls.incrementAndGet();
				return PersistenceResult.APPLIED;
			});

		assertEquals(Status.FAILED, new QuestGraphTransitionExecutor().execute(
			new Match(event, fixture.graph(), fixture.route(), fixture.states().get(1)), context));
		assertEquals(0, persistenceCalls.get());
		assertEquals(Lifecycle.ACTIVE, fixture.states().get(1).getLifecycle());
	}

	/** 验证 codec 保留 authority 快照并拒绝未知 action 或 false authority。 / Verifies codec authority round-trip and rejection of unknown actions or false authority. */
	@Test
	void codecRejectsCorruptInteractionAuthority() {
		InteractionEligibilityEvent event = QuestGraphInteractionEligibilityBridge.event("codec", NOW, snapshot(true));
		byte[] encoded = QuestGraphEventCodec.encode(event);
		assertEquals(event, QuestGraphEventCodec.decode(encoded));
		byte[] unknownAction = Arrays.copyOf(encoded, encoded.length);
		unknownAction[unknownAction.length - 2] = 99;
		assertThrows(IllegalArgumentException.class, () -> QuestGraphEventCodec.decode(unknownAction));
		byte[] falseAuthority = Arrays.copyOf(encoded, encoded.length);
		falseAuthority[falseAuthority.length - 1] = 0;
		assertThrows(IllegalArgumentException.class, () -> QuestGraphEventCodec.decode(falseAuthority));
	}

	private static Fixture fixture() {
		Event event = new Event(CompiledQuestGraph.EventType.INTERACTION_ELIGIBILITY, OBJECT_TEMPLATE_ID,
			InteractionAction.ACTION_ITEM_USE.name());
		Transition transition = new Transition("can-act", 1, "active", event,
			List.of(new QuestStatusCondition(QuestStatus.START), new PlayerLevelCondition(1, null)), List.of());
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, CompiledQuestGraph.StateScope.PLAYER, "active", Map.of(),
			Map.of("active", new Node("active", false, List.of(transition))));
		EventRoute route = new EventRoute(1, "active", transition);
		CompiledQuestGraphData data = new CompiledQuestGraphData(Map.of(1, graph), Map.of(new EventKey(event.type(), event.targetId()), List.of(route)));
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		states.addLoaded(new PlayerQuestGraphState(1, 1, 4, "active", QuestStatus.START, QuestHistory.EMPTY, null,
			Lifecycle.ACTIVE, Map.of(), Map.of(), null, Map.of(), null));
		return new Fixture(graph, route, states, new QuestGraphInteractionEligibilityBridge(new QuestGraphRouter(data)));
	}

	private static InteractionSnapshot snapshot(boolean available) {
		return new InteractionSnapshot(PLAYER_ID, OBJECT_TEMPLATE_ID, 5001, 210010000, 1,
			InteractionAction.ACTION_ITEM_USE, available);
	}

	private record Fixture(CompiledQuestGraph graph, EventRoute route, PlayerQuestGraphStateList states,
			QuestGraphInteractionEligibilityBridge bridge) {
	}
}
