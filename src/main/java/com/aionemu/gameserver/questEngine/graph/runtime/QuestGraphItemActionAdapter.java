package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GiveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.PayKinahAndItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveUsedItemAction;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.ItemUseEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionInvocation;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PreflightResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationKind;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.ItemMutationPlan;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 按冻结 before/after 计划执行并同步持久化任务物品动作。
 * Executes quest-item actions from frozen before/after plans and persists them synchronously.
 */
public final class QuestGraphItemActionAdapter {

	private final int playerId;
	private final Object inventoryLock;
	private final ToLongFunction<Integer> itemCountReader;
	private final Function<Integer, ItemObjectSnapshot> itemObjectReader;
	private final LongSupplier kinahReader;
	private final Predicate<Map<Integer, Long>> grantCapacity;
	private final BiPredicate<Integer, Long> itemGrant;
	private final BiPredicate<Integer, Long> itemRemoval;
	private final BiPredicate<Integer, Long> itemObjectRemoval;
	private final LongPredicate kinahRemoval;
	private final LongConsumer kinahRefund;
	private final BooleanSupplier inventoryPersistence;
	private final IntPredicate alreadyPresentNotifier;

	/**
	 * 创建绑定在线玩家、正式背包服务与 DAO 的生产 adapter。
	 * Creates the production adapter bound to an online player, inventory services, and DAO.
	 */
	public QuestGraphItemActionAdapter(Player player) {
		this(requirePlayer(player).getObjectId(), player.getInventory(), player.getInventory()::getItemCountByItemId,
			objectId -> snapshot(player.getInventory().getItemByObjId(objectId)),
			grants -> ItemService.canAddItems(player, grants),
			(itemId, count) -> count <= Integer.MAX_VALUE && ItemService.addQuestItems(player,
				Collections.singletonList(new QuestItems(itemId, count.intValue()))),
			player.getInventory()::decreaseByItemId, player.getInventory()::decreaseByObjectId,
			player.getInventory()::getKinah, player.getInventory()::tryDecreaseKinah,
			player.getInventory()::increaseKinah,
			() -> DAOManager.getDAO(InventoryDAO.class).store(player),
			itemId -> notifyAlreadyPresent(player, itemId));
	}

	/**
	 * 创建可注入端口的聚焦测试 adapter。
	 * Creates a focused-test adapter with injectable ports.
	 */
	QuestGraphItemActionAdapter(int playerId, Object inventoryLock, ToLongFunction<Integer> itemCountReader,
			Predicate<Map<Integer, Long>> grantCapacity, BiPredicate<Integer, Long> itemGrant, BiPredicate<Integer, Long> itemRemoval,
			BooleanSupplier inventoryPersistence, IntPredicate alreadyPresentNotifier) {
		this(playerId, inventoryLock, itemCountReader, objectId -> null, grantCapacity, itemGrant, itemRemoval,
			(objectId, count) -> false, () -> -1, amount -> false, inventoryPersistence, alreadyPresentNotifier);
	}

	/** 创建包含事件物品对象端口的聚焦测试 adapter。 / Creates a focused-test adapter with event-item object ports. */
	QuestGraphItemActionAdapter(int playerId, Object inventoryLock, ToLongFunction<Integer> itemCountReader,
			Function<Integer, ItemObjectSnapshot> itemObjectReader, Predicate<Map<Integer, Long>> grantCapacity,
			BiPredicate<Integer, Long> itemGrant, BiPredicate<Integer, Long> itemRemoval, BiPredicate<Integer, Long> itemObjectRemoval,
			BooleanSupplier inventoryPersistence, IntPredicate alreadyPresentNotifier) {
		this(playerId, inventoryLock, itemCountReader, itemObjectReader, grantCapacity, itemGrant, itemRemoval, itemObjectRemoval,
			() -> -1, amount -> false, amount -> { }, inventoryPersistence, alreadyPresentNotifier);
	}

	/** 保留无 refund 端口的兼容签名；支付失败时仅显式失败。 / Retains the legacy no-refund signature and fails closed if payment rollback would be required. */
	QuestGraphItemActionAdapter(int playerId, Object inventoryLock, ToLongFunction<Integer> itemCountReader,
			Function<Integer, ItemObjectSnapshot> itemObjectReader, Predicate<Map<Integer, Long>> grantCapacity,
			BiPredicate<Integer, Long> itemGrant, BiPredicate<Integer, Long> itemRemoval, BiPredicate<Integer, Long> itemObjectRemoval,
			LongSupplier kinahReader, LongPredicate kinahRemoval, BooleanSupplier inventoryPersistence, IntPredicate alreadyPresentNotifier) {
		this(playerId, inventoryLock, itemCountReader, itemObjectReader, grantCapacity, itemGrant, itemRemoval, itemObjectRemoval,
			kinahReader, kinahRemoval, amount -> { }, inventoryPersistence, alreadyPresentNotifier);
	}

	/** 创建包含 Kinah 端口的聚焦测试 adapter。 / Creates a focused-test adapter with Kinah ports. */
	QuestGraphItemActionAdapter(int playerId, Object inventoryLock, ToLongFunction<Integer> itemCountReader,
			Function<Integer, ItemObjectSnapshot> itemObjectReader, Predicate<Map<Integer, Long>> grantCapacity,
			BiPredicate<Integer, Long> itemGrant, BiPredicate<Integer, Long> itemRemoval, BiPredicate<Integer, Long> itemObjectRemoval,
			LongSupplier kinahReader, LongPredicate kinahRemoval, LongConsumer kinahRefund,
			BooleanSupplier inventoryPersistence, IntPredicate alreadyPresentNotifier) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Player id is invalid");
		}
		this.playerId = playerId;
		this.inventoryLock = Objects.requireNonNull(inventoryLock, "inventoryLock");
		this.itemCountReader = Objects.requireNonNull(itemCountReader, "itemCountReader");
		this.itemObjectReader = Objects.requireNonNull(itemObjectReader, "itemObjectReader");
		this.kinahReader = Objects.requireNonNull(kinahReader, "kinahReader");
		this.grantCapacity = Objects.requireNonNull(grantCapacity, "grantCapacity");
		this.itemGrant = Objects.requireNonNull(itemGrant, "itemGrant");
		this.itemRemoval = Objects.requireNonNull(itemRemoval, "itemRemoval");
		this.itemObjectRemoval = Objects.requireNonNull(itemObjectRemoval, "itemObjectRemoval");
		this.kinahRemoval = Objects.requireNonNull(kinahRemoval, "kinahRemoval");
		this.kinahRefund = Objects.requireNonNull(kinahRefund, "kinahRefund");
		this.inventoryPersistence = Objects.requireNonNull(inventoryPersistence, "inventoryPersistence");
		this.alreadyPresentNotifier = Objects.requireNonNull(alreadyPresentNotifier, "alreadyPresentNotifier");
	}

	/**
	 * 返回当前背包数量，供转换执行器在 PREPARED 前冻结动作计划。
	 * Returns the current inventory count so the executor can freeze a plan before PREPARED.
	 */
	public long itemCount(int itemId) {
		return itemCountReader.applyAsLong(itemId);
	}

	/** 在 PREPARED 前冻结事件物品扣除或 Kinah+普通物品支付计划。 / Freezes an event-item removal or Kinah-and-ordinary-item payment plan before PREPARED. */
	public ItemMutationPlan prepareUsedItemPlan(ActionInvocation invocation) {
		if (invocation != null && invocation.action() instanceof PayKinahAndItemAction payment) {
			if (!validOwner(invocation)) {
				throw new IllegalArgumentException("Payment invocation belongs to another player");
			}
			synchronized (inventoryLock) {
				long beforeCount = itemCountReader.applyAsLong(payment.itemId());
				long beforeKinah = kinahReader.getAsLong();
				if (beforeCount < payment.itemCount() || beforeKinah < payment.kinah()) {
					return null;
				}
				return ItemMutationPlan.payment(invocation.actionIndex(), payment.itemId(), payment.itemCount(), beforeCount,
					payment.kinah(), beforeKinah);
			}
		}
		if (!validOwner(invocation) || !(invocation.action() instanceof RemoveUsedItemAction action)
				|| !(invocation.event() instanceof ItemUseEvent event)) {
			return null;
		}
		synchronized (inventoryLock) {
			long total = itemCountReader.applyAsLong(event.itemId());
			if (total < action.count()) {
				return null;
			}
			return switch (action.mode()) {
				case EVENT_TEMPLATE_EXACT -> new ItemMutationPlan(invocation.actionIndex(), ItemMutationKind.REMOVE_EVENT_TEMPLATE_EXACT,
					event.itemId(), 0, action.count(), total, Math.subtractExact(total, action.count()), 0);
				case EVENT_OBJECT_EXACT -> {
					ItemObjectSnapshot object = itemObjectReader.apply(event.itemObjectId());
					if (object == null || object.objectId() != event.itemObjectId() || object.itemId() != event.itemId()
							|| object.count() < action.count()) {
						yield null;
					}
					yield new ItemMutationPlan(invocation.actionIndex(), ItemMutationKind.REMOVE_EVENT_OBJECT_EXACT,
						event.itemId(), event.itemObjectId(), action.count(), total, Math.subtractExact(total, action.count()), object.count());
				}
			};
		}
	}

	/**
	 * 预检单个冻结计划的类型和 owner；整组容量由批量入口验证。
	 * Preflights one frozen plan's type and owner; the batch entry validates aggregate capacity.
	 */
	public PreflightResult preflight(ActionInvocation invocation) {
		if (!validOwner(invocation) || invocation.itemMutationPlan() == null
				|| invocation.itemMutationPlan().actionIndex() != invocation.actionIndex()
				|| !matches(invocation.action(), invocation.itemMutationPlan())) {
			return PreflightResult.FAILED;
		}
		return PreflightResult.READY;
	}

	/**
	 * 按动作序号验证整组 before/after 链，并一次检查所有尚未应用的发放容量。
	 * Validates a complete before/after chain by action index and checks aggregate capacity for unapplied grants once.
	 */
	public PreflightResult preflight(Map<Integer, ItemMutationPlan> plans) {
		if (plans == null || plans.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null
				|| entry.getKey().intValue() != entry.getValue().actionIndex())) {
			return PreflightResult.FAILED;
		}
		Map<Integer, Long> projected = new LinkedHashMap<>();
		Map<Integer, Long> grants = new LinkedHashMap<>();
		Long projectedKinah = null;
		synchronized (inventoryLock) {
			for (ItemMutationPlan plan : plans.values().stream().sorted(java.util.Comparator.comparingInt(ItemMutationPlan::actionIndex)).toList()) {
				long current = projected.computeIfAbsent(plan.itemId(), itemCountReader::applyAsLong);
				if (plan.kind() == ItemMutationKind.PAY_KINAH_AND_ITEM) {
					long currentKinah = projectedKinah == null ? kinahReader.getAsLong() : projectedKinah;
					boolean applied = current == plan.afterCount() && currentKinah == plan.afterKinah();
					boolean ready = current == plan.beforeCount()
						&& (currentKinah == plan.beforeKinah() || currentKinah == plan.afterKinah());
					if (!applied && !ready) {
						return PreflightResult.FAILED;
					}
					projected.put(plan.itemId(), plan.afterCount());
					projectedKinah = plan.afterKinah();
					continue;
				}
				if (current == plan.afterCount()) {
					if (!matchesObjectCount(plan, false)) {
						return PreflightResult.FAILED;
					}
					projected.put(plan.itemId(), plan.afterCount());
					continue;
				}
				if (current != plan.beforeCount() || !matchesObjectCount(plan, true)) {
					return PreflightResult.FAILED;
				}
				if ((plan.kind() == ItemMutationKind.GIVE_TOP_UP_TO || plan.kind() == ItemMutationKind.GIVE_ADD_EXACT)
						&& plan.afterCount() > plan.beforeCount()) {
					grants.merge(plan.itemId(), plan.afterCount() - plan.beforeCount(), Math::addExact);
				}
				projected.put(plan.itemId(), plan.afterCount());
			}
			return grantCapacity.test(Map.copyOf(grants)) ? PreflightResult.READY : PreflightResult.REJECTED;
		}
	}

	/**
	 * 严格把当前数量从 before 收敛到 after，并在 journal 推进前同步持久化。
	 * Strictly converges the current count from before to after and persists before journal progress.
	 */
	public ActionResult execute(ActionInvocation invocation) {
		if (!validOwner(invocation) || invocation.itemMutationPlan() == null
				|| invocation.itemMutationPlan().actionIndex() != invocation.actionIndex()
				|| !matches(invocation.action(), invocation.itemMutationPlan())) {
			return ActionResult.FAILED;
		}
		ItemMutationPlan plan = invocation.itemMutationPlan();
		synchronized (inventoryLock) {
			long current = itemCountReader.applyAsLong(plan.itemId());
			if (plan.kind() == ItemMutationKind.PAY_KINAH_AND_ITEM) {
				return executePayment(plan, current);
			}
			if (current == plan.afterCount()) {
				if (!matchesObjectCount(plan, false)) {
					return ActionResult.FAILED;
				}
				if (plan.kind() == ItemMutationKind.GIVE_TOP_UP_TO && plan.beforeCount() == plan.afterCount()
						&& !alreadyPresentNotifier.test(plan.itemId())) {
					return ActionResult.FAILED;
				}
				return plan.beforeCount() == plan.afterCount() || inventoryPersistence.getAsBoolean()
					? ActionResult.ALREADY_APPLIED : ActionResult.FAILED;
			}
			if (current != plan.beforeCount() || !matchesObjectCount(plan, true)) {
				return ActionResult.FAILED;
			}
			boolean changed = switch (plan.kind()) {
				case PAY_KINAH_AND_ITEM -> false;
				case GIVE_TOP_UP_TO, GIVE_ADD_EXACT -> itemGrant.test(plan.itemId(), plan.afterCount() - plan.beforeCount());
				case REMOVE_EXACT, REMOVE_OPTIONAL_EXACT -> itemRemoval.test(plan.itemId(), plan.requestedCount());
				case REMOVE_ALL -> itemRemoval.test(plan.itemId(), plan.beforeCount());
				case REMOVE_EVENT_OBJECT_EXACT -> itemObjectRemoval.test(plan.itemObjectId(), plan.requestedCount());
				case REMOVE_EVENT_TEMPLATE_EXACT -> itemRemoval.test(plan.itemId(), plan.requestedCount());
			};
			if (!changed || itemCountReader.applyAsLong(plan.itemId()) != plan.afterCount() || !matchesObjectCount(plan, false)) {
				return ActionResult.FAILED;
			}
			return inventoryPersistence.getAsBoolean() ? ActionResult.APPLIED : ActionResult.FAILED;
		}
	}

	/** 从全 before 或仅 Kinah 已扣的恢复中间态收敛到全 after。 / Converges from all-before or the Kinah-only recovery midpoint to all-after. */
	private ActionResult executePayment(ItemMutationPlan plan, long currentItemCount) {
		long currentKinah = kinahReader.getAsLong();
		if (currentItemCount == plan.afterCount() && currentKinah == plan.afterKinah()) {
			return inventoryPersistence.getAsBoolean() ? ActionResult.ALREADY_APPLIED : ActionResult.FAILED;
		}
		if (currentItemCount != plan.beforeCount()
				|| currentKinah != plan.beforeKinah() && currentKinah != plan.afterKinah()) {
			return ActionResult.FAILED;
		}
		if (currentKinah == plan.beforeKinah()
				&& (!kinahRemoval.test(plan.kinahAmount()) || kinahReader.getAsLong() != plan.afterKinah())) {
			return ActionResult.FAILED;
		}
		boolean itemChanged = itemRemoval.test(plan.itemId(), plan.requestedCount());
		long afterItemCount = itemCountReader.applyAsLong(plan.itemId());
		long afterKinah = kinahReader.getAsLong();
		if (!itemChanged || afterItemCount != plan.afterCount() || afterKinah != plan.afterKinah()) {
			// The inventory lock makes the refund deterministic for an atomic item-removal failure.
			if (afterKinah == plan.afterKinah() && afterItemCount == plan.beforeCount()) {
				kinahRefund.accept(plan.kinahAmount());
			}
			return ActionResult.FAILED;
		}
		return inventoryPersistence.getAsBoolean() ? ActionResult.APPLIED : ActionResult.FAILED;
	}

	/** 校验调用与 adapter 玩家一致。 / Validates that the invocation belongs to the adapter's player. */
	private boolean validOwner(ActionInvocation invocation) {
		return invocation != null && invocation.event().playerId() == playerId;
	}

	/** 校验冻结计划与强类型动作完全一致。 / Validates exact agreement between a frozen plan and typed action. */
	private static boolean matches(Action action, ItemMutationPlan plan) {
		if (plan == null) {
			return false;
		}
		if (action instanceof GiveQuestItemAction give) {
			ItemMutationKind expected = switch (give.mode()) {
				case TOP_UP_TO -> ItemMutationKind.GIVE_TOP_UP_TO;
				case ADD_EXACT -> ItemMutationKind.GIVE_ADD_EXACT;
			};
			return plan.kind() == expected && plan.itemId() == give.itemId()
				&& plan.requestedCount() == give.count();
		}
		if (action instanceof RemoveQuestItemAction remove) {
			ItemMutationKind expected = switch (remove.mode()) {
				case EXACT -> ItemMutationKind.REMOVE_EXACT;
				case OPTIONAL_EXACT -> ItemMutationKind.REMOVE_OPTIONAL_EXACT;
				case ALL -> ItemMutationKind.REMOVE_ALL;
			};
			return plan.kind() == expected && plan.itemId() == remove.itemId()
				&& plan.requestedCount() == remove.count();
		}
		if (action instanceof RemoveUsedItemAction remove) {
			ItemMutationKind expected = switch (remove.mode()) {
				case EVENT_OBJECT_EXACT -> ItemMutationKind.REMOVE_EVENT_OBJECT_EXACT;
				case EVENT_TEMPLATE_EXACT -> ItemMutationKind.REMOVE_EVENT_TEMPLATE_EXACT;
			};
			return plan.kind() == expected && plan.requestedCount() == remove.count();
		}
		if (action instanceof PayKinahAndItemAction payment) {
			return plan.kind() == ItemMutationKind.PAY_KINAH_AND_ITEM && plan.itemId() == payment.itemId()
				&& plan.requestedCount() == payment.itemCount() && plan.kinahAmount() == payment.kinah();
		}
		return false;
	}

	/** 比较冻结对象栈在动作前或动作后的精确数量；模板模式不要求对象身份。 / Compares the exact frozen object stack before or after mutation. */
	private boolean matchesObjectCount(ItemMutationPlan plan, boolean before) {
		if (plan.kind() != ItemMutationKind.REMOVE_EVENT_OBJECT_EXACT) {
			return plan.itemObjectId() == 0 && plan.beforeObjectCount() == 0;
		}
		ItemObjectSnapshot object = itemObjectReader.apply(plan.itemObjectId());
		long expected = before ? plan.beforeObjectCount() : plan.beforeObjectCount() - plan.requestedCount();
		if (expected == 0) {
			return object == null;
		}
		return object != null && object.objectId() == plan.itemObjectId() && object.itemId() == plan.itemId()
			&& object.count() == expected;
	}

	private static ItemObjectSnapshot snapshot(Item item) {
		return item == null ? null : new ItemObjectSnapshot(item.getObjectId(), item.getItemId(), item.getItemCount());
	}

	/** 服务端背包中一个具体物品对象的不可变快照。 / Immutable snapshot of one concrete server inventory item. */
	record ItemObjectSnapshot(int objectId, int itemId, long count) {
		ItemObjectSnapshot {
			if (objectId <= 0 || itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("Item object snapshot is invalid");
			}
		}
	}

	/** 返回已校验的玩家。 / Returns a validated player. */
	private static Player requirePlayer(Player player) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(player.getInventory(), "player inventory");
		return player;
	}

	/** 复用旧 helper 的“已持有足量 lore item”提示。 / Reuses the legacy helper's lore-item already-present message. */
	private static boolean notifyAlreadyPresent(Player player, int itemId) {
		ItemTemplate item = DataManager.ITEM_DATA == null ? null : DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (item == null) {
			return false;
		}
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CAN_NOT_GET_LORE_ITEM(new com.aionemu.gameserver.model.DescriptionId(item.getNameId())));
		return true;
	}
}
