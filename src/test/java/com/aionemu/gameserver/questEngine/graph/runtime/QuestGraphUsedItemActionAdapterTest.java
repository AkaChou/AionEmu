package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveUsedItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.UsedItemRemovalMode;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphItemActionAdapter.ItemObjectSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

class QuestGraphUsedItemActionAdapterTest {

	private static final int ITEM_ID = 182206034;
	private static final int ITEM_OBJECT_ID = 55;

	@Test
	void removesOnlyFrozenEventObjectAndConvergesIdempotently() {
		AtomicLong total = new AtomicLong(5);
		AtomicReference<ItemObjectSnapshot> object = new AtomicReference<>(new ItemObjectSnapshot(ITEM_OBJECT_ID, ITEM_ID, 3));
		AtomicInteger persistence = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = adapter(total, object, persistence);
		RemoveUsedItemAction action = new RemoveUsedItemAction(1, UsedItemRemovalMode.EVENT_OBJECT_EXACT);
		ActionInvocation unresolved = invocation(action, null, "object-remove");
		ItemMutationPlan plan = adapter.prepareUsedItemPlan(unresolved);
		ActionInvocation frozen = invocation(action, plan, "object-remove");

		assertEquals(PreflightResult.READY, adapter.preflight(frozen));
		assertEquals(PreflightResult.READY, adapter.preflight(Map.of(1, plan)));
		assertEquals(ActionResult.APPLIED, adapter.execute(frozen));
		assertEquals(4, total.get());
		assertEquals(2, object.get().count());
		assertEquals(ActionResult.ALREADY_APPLIED, adapter.execute(frozen));
		assertEquals(2, persistence.get());
	}

	@Test
	void removesByFrozenEventTemplateWithoutBindingAnObject() {
		AtomicLong total = new AtomicLong(5);
		AtomicReference<ItemObjectSnapshot> object = new AtomicReference<>(new ItemObjectSnapshot(ITEM_OBJECT_ID, ITEM_ID, 3));
		QuestGraphItemActionAdapter adapter = adapter(total, object, new AtomicInteger());
		RemoveUsedItemAction action = new RemoveUsedItemAction(1, UsedItemRemovalMode.EVENT_TEMPLATE_EXACT);
		ItemMutationPlan plan = adapter.prepareUsedItemPlan(invocation(action, null, "template-remove"));

		assertEquals(0, plan.itemObjectId());
		assertEquals(ActionResult.APPLIED, adapter.execute(invocation(action, plan, "template-remove")));
		assertEquals(4, total.get());
		assertEquals(3, object.get().count());
	}

	@Test
	void rejectsObjectOrTemplateDriftAfterThePlanWasFrozen() {
		AtomicLong total = new AtomicLong(5);
		AtomicReference<ItemObjectSnapshot> object = new AtomicReference<>(new ItemObjectSnapshot(ITEM_OBJECT_ID, ITEM_ID, 3));
		AtomicInteger persistence = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = adapter(total, object, persistence);
		RemoveUsedItemAction objectAction = new RemoveUsedItemAction(1, UsedItemRemovalMode.EVENT_OBJECT_EXACT);
		ItemMutationPlan objectPlan = adapter.prepareUsedItemPlan(invocation(objectAction, null, "object-drift"));
		object.set(new ItemObjectSnapshot(ITEM_OBJECT_ID, ITEM_ID, 2));

		assertEquals(PreflightResult.FAILED, adapter.preflight(Map.of(1, objectPlan)));
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(objectAction, objectPlan, "object-drift")));

		object.set(new ItemObjectSnapshot(ITEM_OBJECT_ID, ITEM_ID, 3));
		RemoveUsedItemAction templateAction = new RemoveUsedItemAction(1, UsedItemRemovalMode.EVENT_TEMPLATE_EXACT);
		ItemMutationPlan templatePlan = adapter.prepareUsedItemPlan(invocation(templateAction, null, "template-drift"));
		total.set(6);
		assertEquals(PreflightResult.FAILED, adapter.preflight(Map.of(1, templatePlan)));
		assertEquals(ActionResult.FAILED, adapter.execute(invocation(templateAction, templatePlan, "template-drift")));
		assertEquals(0, persistence.get());
	}

	private static QuestGraphItemActionAdapter adapter(AtomicLong total, AtomicReference<ItemObjectSnapshot> object,
			AtomicInteger persistence) {
		return new QuestGraphItemActionAdapter(7, new Object(), itemId -> total.get(), objectId -> object.get(), grants -> true,
			(itemId, count) -> false, (itemId, count) -> {
				total.addAndGet(-count);
				return true;
			}, (objectId, count) -> {
				ItemObjectSnapshot before = object.get();
				long after = before.count() - count;
				object.set(after == 0 ? null : new ItemObjectSnapshot(before.objectId(), before.itemId(), after));
				total.addAndGet(-count);
				return true;
			}, () -> {
				persistence.incrementAndGet();
				return true;
			}, itemId -> true);
	}

	private static ActionInvocation invocation(RemoveUsedItemAction action, ItemMutationPlan plan, String key) {
		return new ActionInvocation(action, 1, 1, QuestStatus.NONE,
			new ItemUseEvent("item-use-event", 7, 1_700_000_000_000L, ITEM_ID, ITEM_OBJECT_ID),
			RepeatDeadlineResolution.NOT_APPLICABLE, plan, key);
	}
}
