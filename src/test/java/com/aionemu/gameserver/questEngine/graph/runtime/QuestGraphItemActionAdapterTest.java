package com.aionemu.gameserver.questEngine.graph.runtime;

import static com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus.START;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.ALREADY_APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.APPLIED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult.FAILED;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.READY;
import static com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult.REJECTED;
import static com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind.GIVE_TOP_UP_TO;
import static com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind.GIVE_ADD_EXACT;
import static com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind.REMOVE_EXACT;
import static com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind.REMOVE_OPTIONAL_EXACT;
import static com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind.REMOVE_ALL;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GiveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PayKinahAndItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.DialogEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationPlan;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.RepeatDeadlineResolution;

/**
 * 验证冻结物品计划的严格收敛、同步持久化和恢复行为。
 * Verifies strict convergence, synchronous persistence, and recovery of frozen item plans.
 */
class QuestGraphItemActionAdapterTest {

	private static final DialogEvent EVENT = new DialogEvent("event", 7, 1000, 100, "QUEST_SELECT");

	/** 验证 give 从 before 补齐到 after，恢复时不重复发放但仍确认库存已持久化。 / Verifies give convergence and recovery without a duplicate grant. */
	@Test
	void givePersistsBeforeJournalProgressAndReplaysWithoutDuplicateGrant() {
		AtomicLong count = new AtomicLong(2);
		AtomicInteger grants = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = adapter(count, grants, new AtomicInteger(), stores);
		ItemMutationPlan plan = new ItemMutationPlan(0, GIVE_TOP_UP_TO, 182200001, 5, 2, 5);
		ActionInvocation invocation = invocation(new GiveQuestItemAction(182200001, 5,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemGrantMode.TOP_UP_TO), plan);

		assertEquals(READY, adapter.preflight(invocation));
		assertEquals(APPLIED, adapter.execute(invocation));
		assertEquals(5, count.get());
		assertEquals(1, grants.get());
		assertEquals(1, stores.get());
		assertEquals(ALREADY_APPLIED, adapter.execute(invocation));
		assertEquals(1, grants.get());
		assertEquals(2, stores.get());
	}

	/** 验证 ADD_EXACT 从冻结 before 追加显式数量，恢复时只收敛到同一 after。 / Verifies ADD_EXACT appends an explicit count from frozen before and converges to the same after on recovery. */
	@Test
	void additiveGivePreservesExistingWorkOrderMaterials() {
		AtomicLong count = new AtomicLong(2);
		AtomicInteger grants = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = adapter(count, grants, new AtomicInteger(), stores);
		ItemMutationPlan plan = new ItemMutationPlan(0, GIVE_ADD_EXACT, 182200001, 4, 2, 6);
		ActionInvocation invocation = invocation(new GiveQuestItemAction(182200001, 4,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemGrantMode.ADD_EXACT), plan);

		assertEquals(READY, adapter.preflight(invocation));
		assertEquals(APPLIED, adapter.execute(invocation));
		assertEquals(6, count.get());
		assertEquals(1, grants.get());
		assertEquals(ALREADY_APPLIED, adapter.execute(invocation));
		assertEquals(1, grants.get());
	}

	/** 验证 exact remove 只接受 before/after 两端，任何外部数量漂移均显式失败。 / Verifies exact remove accepts only the before/after endpoints. */
	@Test
	void exactRemoveRejectsDivergedInventory() {
		AtomicLong count = new AtomicLong(5);
		AtomicInteger removals = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = adapter(count, new AtomicInteger(), removals, new AtomicInteger());
		ItemMutationPlan plan = new ItemMutationPlan(0, REMOVE_EXACT, 182200001, 2, 5, 3);
		ActionInvocation invocation = invocation(new RemoveQuestItemAction(182200001, 2,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemRemovalMode.EXACT), plan);

		assertEquals(APPLIED, adapter.execute(invocation));
		assertEquals(3, count.get());
		assertEquals(1, removals.get());
		count.set(4);
		assertEquals(FAILED, adapter.execute(invocation));
		assertEquals(1, removals.get());
	}

	/**
	 * 验证库存不足的 optional exact 是无副作用 no-op，且模式不匹配的冻结计划失败。
	 * Verifies that an insufficient optional exact removal is a side-effect-free no-op and a mismatched frozen plan fails.
	 */
	@Test
	void insufficientOptionalExactRemovalIsSideEffectFree() {
		AtomicLong count = new AtomicLong(1);
		AtomicInteger removals = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		AtomicInteger notifications = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = new QuestGraphItemActionAdapter(7, new Object(), ignored -> count.get(), values -> true,
			(itemId, delta) -> true, (itemId, delta) -> {
				removals.incrementAndGet();
				return true;
			}, () -> {
				stores.incrementAndGet();
				return true;
			}, itemId -> {
				notifications.incrementAndGet();
				return true;
			});
		RemoveQuestItemAction action = new RemoveQuestItemAction(182200001, 2,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemRemovalMode.OPTIONAL_EXACT);
		ItemMutationPlan plan = new ItemMutationPlan(0, REMOVE_OPTIONAL_EXACT, 182200001, 2, 1, 1);

		assertEquals(READY, adapter.preflight(invocation(action, plan)));
		assertEquals(ALREADY_APPLIED, adapter.execute(invocation(action, plan)));
		assertEquals(0, removals.get());
		assertEquals(0, stores.get());
		assertEquals(0, notifications.get());
		assertEquals(QuestGraphTransitionExecutor.PreflightResult.FAILED, adapter.preflight(invocation(action,
			new ItemMutationPlan(0, REMOVE_EXACT, 182200001, 2, 3, 1))));
	}

	/**
	 * 验证足量 optional exact 仍精确扣除并持久化，外部库存漂移显式失败。
	 * Verifies that a sufficient optional exact removal still removes exactly and persists, while inventory drift fails.
	 */
	@Test
	void sufficientOptionalExactRemovalRejectsInventoryDrift() {
		AtomicLong count = new AtomicLong(5);
		AtomicInteger removals = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = adapter(count, new AtomicInteger(), removals, stores);
		ItemMutationPlan plan = new ItemMutationPlan(0, REMOVE_OPTIONAL_EXACT, 182200001, 2, 5, 3);
		ActionInvocation invocation = invocation(new RemoveQuestItemAction(182200001, 2,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemRemovalMode.OPTIONAL_EXACT), plan);

		assertEquals(APPLIED, adapter.execute(invocation));
		assertEquals(3, count.get());
		assertEquals(1, removals.get());
		assertEquals(1, stores.get());
		count.set(4);
		assertEquals(FAILED, adapter.execute(invocation));
		assertEquals(1, removals.get());
		assertEquals(1, stores.get());
	}

	/** 验证全部扣除使用冻结 before 数量，并把库存严格收敛到零。 / Verifies remove-all uses frozen before count and converges to zero. */
	@Test
	void removeAllUsesFrozenInventoryCount() {
		AtomicLong count = new AtomicLong(7);
		AtomicInteger removals = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = adapter(count, new AtomicInteger(), removals, stores);
		ItemMutationPlan plan = new ItemMutationPlan(0, REMOVE_ALL, 182200001, 1, 7, 0);
		ActionInvocation invocation = invocation(new RemoveQuestItemAction(182200001, 1,
			com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestItemRemovalMode.ALL), plan);

		assertEquals(APPLIED, adapter.execute(invocation));
		assertEquals(0, count.get());
		assertEquals(1, removals.get());
		assertEquals(1, stores.get());
	}

	/** 验证整组预检按 action index 投影同一物品的连续 give/remove。 / Verifies batch preflight projects consecutive give/remove actions by index. */
	@Test
	void batchPreflightProjectsConsecutiveMutations() {
		AtomicLong count = new AtomicLong(2);
		AtomicReference<Map<Integer, Long>> grants = new AtomicReference<>();
		QuestGraphItemActionAdapter adapter = new QuestGraphItemActionAdapter(7, new Object(), ignored -> count.get(), values -> {
			grants.set(values);
			return true;
		}, (itemId, delta) -> true, (itemId, delta) -> true, () -> true, itemId -> true);

		assertEquals(READY, adapter.preflight(Map.of(
			0, new ItemMutationPlan(0, GIVE_TOP_UP_TO, 182200001, 5, 2, 5),
			1, new ItemMutationPlan(1, REMOVE_EXACT, 182200001, 2, 5, 3))));
		assertEquals(Map.of(182200001, 3L), grants.get());
	}

	/** 验证整组 give 容量不足在写 PREPARED 前作为业务拒绝返回。 / Verifies insufficient aggregate give capacity rejects before PREPARED. */
	@Test
	void batchPreflightRejectsInsufficientGrantCapacity() {
		QuestGraphItemActionAdapter adapter = new QuestGraphItemActionAdapter(7, new Object(), ignored -> 0,
			values -> false, (itemId, delta) -> true, (itemId, delta) -> true, () -> true, itemId -> true);

		assertEquals(REJECTED, adapter.preflight(Map.of(
			0, new ItemMutationPlan(0, GIVE_TOP_UP_TO, 182200001, 5, 0, 5))));
	}

	/** 验证 Kinah 与普通物品支付在同一冻结计划中收敛，并且恢复不会重复扣款。 / Verifies Kinah-and-item payment converges from one frozen plan and recovery does not double-charge. */
	@Test
	void paymentConvergesAndRecoversWithoutDoubleCharge() {
		AtomicLong itemCount = new AtomicLong(5);
		AtomicLong kinah = new AtomicLong(100);
		AtomicInteger itemRemovals = new AtomicInteger();
		AtomicInteger kinahRemovals = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = paymentAdapter(itemCount, kinah, itemRemovals, kinahRemovals, stores, true);
		PayKinahAndItemAction action = new PayKinahAndItemAction(40, 182200001, 2);
		ItemMutationPlan plan = ItemMutationPlan.payment(0, 182200001, 2, 5, 40, 100);
		ActionInvocation invocation = invocation(action, plan);

		assertEquals(READY, adapter.preflight(invocation));
		assertEquals(FAILED, adapter.preflight(new ActionInvocation(action, 1, 1, START, EVENT,
			RepeatDeadlineResolution.NOT_APPLICABLE, plan, "wrong-index")));
		assertEquals(APPLIED, adapter.execute(invocation));
		assertEquals(3, itemCount.get());
		assertEquals(60, kinah.get());
		assertEquals(1, itemRemovals.get());
		assertEquals(1, kinahRemovals.get());
		assertEquals(ALREADY_APPLIED, adapter.execute(invocation));
		assertEquals(1, itemRemovals.get());
		assertEquals(1, kinahRemovals.get());
	}

	/** 验证 Kinah 已扣、物品尚未扣除的崩溃中间态可恢复为完整支付。 / Verifies recovery from the Kinah-deducted/item-not-deducted crash midpoint. */
	@Test
	void paymentRecoveryCompletesKinahOnlyMidpoint() {
		AtomicLong itemCount = new AtomicLong(5);
		AtomicLong kinah = new AtomicLong(60);
		AtomicInteger itemRemovals = new AtomicInteger();
		AtomicInteger stores = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = paymentAdapter(itemCount, kinah, itemRemovals, new AtomicInteger(), stores, true);
		ItemMutationPlan plan = ItemMutationPlan.payment(0, 182200001, 2, 5, 40, 100);

		assertEquals(APPLIED, adapter.execute(invocation(new PayKinahAndItemAction(40, 182200001, 2), plan)));
		assertEquals(3, itemCount.get());
		assertEquals(60, kinah.get());
		assertEquals(1, itemRemovals.get());
		assertEquals(1, stores.get());
	}

	/** 验证物品扣除失败时会恢复已扣 Kinah，并把动作留在失败态供 journal 重试。 / Verifies failed item removal refunds Kinah and leaves the action failed for journal retry. */
	@Test
	void failedPaymentItemRemovalRefundsKinah() {
		AtomicLong itemCount = new AtomicLong(5);
		AtomicLong kinah = new AtomicLong(100);
		AtomicInteger refunds = new AtomicInteger();
		QuestGraphItemActionAdapter adapter = paymentAdapter(itemCount, kinah, new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), false,
			refunds);
		ItemMutationPlan plan = ItemMutationPlan.payment(0, 182200001, 2, 5, 40, 100);

		assertEquals(FAILED, adapter.execute(invocation(new PayKinahAndItemAction(40, 182200001, 2), plan)));
		assertEquals(5, itemCount.get());
		assertEquals(100, kinah.get());
		assertEquals(1, refunds.get());
	}

	/** 创建由原子计数器驱动的 adapter。 / Creates an adapter backed by atomic counters. */
	private static QuestGraphItemActionAdapter adapter(AtomicLong count, AtomicInteger grants, AtomicInteger removals, AtomicInteger stores) {
		return new QuestGraphItemActionAdapter(7, new Object(), ignored -> count.get(), values -> true, (itemId, delta) -> {
			grants.incrementAndGet();
			count.addAndGet(delta);
			return true;
		}, (itemId, delta) -> {
			removals.incrementAndGet();
			count.addAndGet(-delta);
			return true;
		}, () -> {
			stores.incrementAndGet();
			return true;
		}, itemId -> true);
	}

	/** 创建包含 Kinah 原子扣除和可控物品扣除结果的 adapter。 / Creates an adapter with atomic Kinah deduction and controllable item-removal outcomes. */
	private static QuestGraphItemActionAdapter paymentAdapter(AtomicLong itemCount, AtomicLong kinah, AtomicInteger itemRemovals,
		AtomicInteger kinahRemovals, AtomicInteger stores, boolean itemRemovalSucceeds) {
		return paymentAdapter(itemCount, kinah, itemRemovals, kinahRemovals, stores, itemRemovalSucceeds, new AtomicInteger());
	}

	/** 创建可观察退款次数的支付 adapter。 / Creates a payment adapter with observable refund count. */
	private static QuestGraphItemActionAdapter paymentAdapter(AtomicLong itemCount, AtomicLong kinah, AtomicInteger itemRemovals,
		AtomicInteger kinahRemovals, AtomicInteger stores, boolean itemRemovalSucceeds, AtomicInteger refunds) {
		return new QuestGraphItemActionAdapter(7, new Object(), ignored -> itemCount.get(), objectId -> null, values -> true,
			(itemId, delta) -> true, (itemId, delta) -> {
				itemRemovals.incrementAndGet();
				if (itemRemovalSucceeds) {
					itemCount.addAndGet(-delta);
				}
				return itemRemovalSucceeds;
			}, (objectId, delta) -> false, kinah::get, amount -> {
				if (kinah.get() < amount) {
					return false;
				}
				kinah.addAndGet(-amount);
				kinahRemovals.incrementAndGet();
				return true;
			}, amount -> {
				kinah.addAndGet(amount);
				refunds.incrementAndGet();
			}, () -> {
				stores.incrementAndGet();
				return true;
			}, itemId -> true);
	}

	/** 创建物品动作调用。 / Creates an item-action invocation. */
	private static ActionInvocation invocation(com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action action,
			ItemMutationPlan plan) {
		return new ActionInvocation(action, 1, plan.actionIndex(), START, EVENT, RepeatDeadlineResolution.NOT_APPLICABLE, plan, "key");
	}
}
