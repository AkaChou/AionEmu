package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.DelayItemUseContinuationAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Event;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EventType;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Node;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PlayMovieAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveUsedItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SetQuestStatusAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StateScope;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Transition;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.UsedItemRemovalMode;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData.EventRoute;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemActionAdapter.ItemObjectSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemUseAnimationAdapter.ScheduleResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphRouter.Match;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.TransitionContext;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.Lifecycle;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

class QuestGraphDelayedItemUseIntegrationTest {

	@Test
	void resumesDurableTailAndCommitsStateBeforeProtocol() {
		long occurredAt = 1_700_000_000_000L;
		AtomicLong now = new AtomicLong(occurredAt + 1000);
		AtomicLong total = new AtomicLong(2);
		AtomicReference<ItemObjectSnapshot> object = new AtomicReference<>(new ItemObjectSnapshot(55, 182206034, 2));
		AtomicInteger starts = new AtomicInteger();
		AtomicInteger ends = new AtomicInteger();
		AtomicInteger movies = new AtomicInteger();
		List<String> order = new ArrayList<>();
		AtomicReference<Runnable> wakeup = new AtomicReference<>();
		AtomicReference<PlayerQuestGraphState> database = new AtomicReference<>();
		PlayerQuestGraphStateList states = new PlayerQuestGraphStateList();
		QuestGraphTransitionExecutor executor = new QuestGraphTransitionExecutor();
		Transition transition = new Transition("use-item", 10, "done", new Event(EventType.ITEM_USE, 182206034, null), List.of(),
			List.of(new DelayItemUseContinuationAction(3000),
				new RemoveUsedItemAction(1, UsedItemRemovalMode.EVENT_OBJECT_EXACT),
				new SetQuestStatusAction(QuestStatus.START), new PlayMovieAction(913)));
		CompiledQuestGraph graph = new CompiledQuestGraph(1, 1, StateScope.PLAYER, "offer", Map.of(),
			Map.of("offer", new Node("offer", false, List.of(transition)), "done", new Node("done", true, List.of())));
		QuestGraphItemActionAdapter items = itemAdapter(total, object, order);
		AtomicReference<TransitionContext> contextRef = new AtomicReference<>();
		QuestGraphItemUseAnimationAdapter animations = new QuestGraphItemUseAnimationAdapter(7, command -> {
			starts.incrementAndGet();
			order.add("start");
			return ActionResult.APPLIED;
		}, (command, finish) -> new ScheduleResult(ActionResult.FAILED, null), command -> ActionResult.FAILED,
			(command, callback, delayMillis) -> {
				assertEquals(2000, delayMillis);
				wakeup.set(callback);
				return new ScheduleResult(ActionResult.APPLIED, () -> {
				});
			}, command -> executor.recover(graph, contextRef.get()) == DispatchResult.Status.APPLIED
				? ActionResult.APPLIED : ActionResult.FAILED, command -> {
					ends.incrementAndGet();
					order.add("end");
					return ActionResult.APPLIED;
				}, now::get);
		TransitionContext context = new TransitionContext(7, 0, ZoneId.of("UTC"), states,
			invocation -> QuestGraphTransitionExecutor.ConditionResult.MATCHED,
			invocation -> invocation.itemMutationPlan() != null ? items.preflight(invocation)
				: invocation.action() instanceof DelayItemUseContinuationAction ? animations.preflight(invocation)
					: PreflightResult.READY,
			invocation -> invocation.itemMutationPlan() != null ? items.execute(invocation)
				: invocation.action() instanceof DelayItemUseContinuationAction ? animations.execute(invocation)
					: invocation.action() instanceof PlayMovieAction ? applied(movies, order, states) : ActionResult.APPLIED,
			items::itemCount, items::preflight, items::prepareUsedItemPlan,
			invocation -> null, invocation -> null, invocation -> null, invocation -> ActionResult.ALREADY_APPLIED,
			(expected, next) -> compareAndSet(database, expected, next));
		contextRef.set(context);
		ItemUseEvent event = new ItemUseEvent("item-use-event", 7, occurredAt, 182206034, 55);

		assertEquals(DispatchResult.Status.APPLIED, executor.execute(
			new Match(event, graph, new EventRoute(1, "offer", transition), null), context));
		assertEquals(Lifecycle.PREPARED, states.get(1).getLifecycle());
		assertEquals(0, states.get(1).getJournal().getNextActionIndex());
		assertEquals(1, starts.get());
		assertEquals(2, total.get());

		now.set(occurredAt + 3000);
		wakeup.get().run();
		assertEquals(1, ends.get());
		assertEquals(1, movies.get());
		assertEquals(1, total.get());
		assertEquals(1, object.get().count());
		assertEquals(Lifecycle.ACTIVE, states.get(1).getLifecycle());
		assertEquals(QuestStatus.START, states.get(1).getQuestStatus());
		assertEquals("done", states.get(1).getNodeId());
		assertEquals(List.of("start", "end", "remove", "movie"), order);
	}

	private static QuestGraphItemActionAdapter itemAdapter(AtomicLong total, AtomicReference<ItemObjectSnapshot> object,
			List<String> order) {
		return new QuestGraphItemActionAdapter(7, new Object(), itemId -> total.get(), objectId -> object.get(), grants -> true,
			(itemId, count) -> false, (itemId, count) -> false, (objectId, count) -> {
				ItemObjectSnapshot before = object.get();
				object.set(new ItemObjectSnapshot(before.objectId(), before.itemId(), before.count() - count));
				total.addAndGet(-count);
				order.add("remove");
				return true;
			}, () -> true, itemId -> true);
	}

	private static ActionResult applied(AtomicInteger calls, List<String> order, PlayerQuestGraphStateList states) {
		assertEquals(QuestStatus.START, states.get(1).getQuestStatus());
		calls.incrementAndGet();
		order.add("movie");
		return ActionResult.APPLIED;
	}

	private static PersistenceResult compareAndSet(AtomicReference<PlayerQuestGraphState> database, Long expected,
			PlayerQuestGraphState next) {
		PlayerQuestGraphState current = database.get();
		if (expected == null ? current != null : current == null || current.getRevision() != expected) {
			return PersistenceResult.CONFLICT;
		}
		database.set(next);
		return PersistenceResult.APPLIED;
	}
}
