package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.Action;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.GiveQuestItemAction;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.RemoveQuestItemAction;
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
	private final Predicate<Map<Integer, Long>> grantCapacity;
	private final BiPredicate<Integer, Long> itemGrant;
	private final BiPredicate<Integer, Long> itemRemoval;
	private final BooleanSupplier inventoryPersistence;
	private final IntPredicate alreadyPresentNotifier;

	/**
	 * 创建绑定在线玩家、正式背包服务与 DAO 的生产 adapter。
	 * Creates the production adapter bound to an online player, inventory services, and DAO.
	 */
	public QuestGraphItemActionAdapter(Player player) {
		this(requirePlayer(player).getObjectId(), player.getInventory(), player.getInventory()::getItemCountByItemId,
			grants -> ItemService.canAddItems(player, grants),
			(itemId, count) -> count <= Integer.MAX_VALUE && ItemService.addQuestItems(player,
				Collections.singletonList(new QuestItems(itemId, count.intValue()))),
			player.getInventory()::decreaseByItemId,
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
		if (playerId <= 0) {
			throw new IllegalArgumentException("Player id is invalid");
		}
		this.playerId = playerId;
		this.inventoryLock = Objects.requireNonNull(inventoryLock, "inventoryLock");
		this.itemCountReader = Objects.requireNonNull(itemCountReader, "itemCountReader");
		this.grantCapacity = Objects.requireNonNull(grantCapacity, "grantCapacity");
		this.itemGrant = Objects.requireNonNull(itemGrant, "itemGrant");
		this.itemRemoval = Objects.requireNonNull(itemRemoval, "itemRemoval");
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

	/**
	 * 预检单个冻结计划的类型和 owner；整组容量由批量入口验证。
	 * Preflights one frozen plan's type and owner; the batch entry validates aggregate capacity.
	 */
	public PreflightResult preflight(ActionInvocation invocation) {
		if (!validOwner(invocation) || !matches(invocation.action(), invocation.itemMutationPlan())) {
			return PreflightResult.FAILED;
		}
		return PreflightResult.READY;
	}

	/**
	 * 按动作序号验证整组 before/after 链，并一次检查所有尚未应用的发放容量。
	 * Validates a complete before/after chain by action index and checks aggregate capacity for unapplied grants once.
	 */
	public PreflightResult preflight(Map<Integer, ItemMutationPlan> plans) {
		Map<Integer, Long> projected = new LinkedHashMap<>();
		Map<Integer, Long> grants = new LinkedHashMap<>();
		synchronized (inventoryLock) {
			for (ItemMutationPlan plan : plans.values().stream().sorted(java.util.Comparator.comparingInt(ItemMutationPlan::actionIndex)).toList()) {
				long current = projected.computeIfAbsent(plan.itemId(), itemCountReader::applyAsLong);
				if (current == plan.afterCount()) {
					projected.put(plan.itemId(), plan.afterCount());
					continue;
				}
				if (current != plan.beforeCount()) {
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
		if (!validOwner(invocation) || !matches(invocation.action(), invocation.itemMutationPlan())) {
			return ActionResult.FAILED;
		}
		ItemMutationPlan plan = invocation.itemMutationPlan();
		synchronized (inventoryLock) {
			long current = itemCountReader.applyAsLong(plan.itemId());
			if (current == plan.afterCount()) {
				if (plan.kind() == ItemMutationKind.GIVE_TOP_UP_TO && plan.beforeCount() == plan.afterCount()
						&& !alreadyPresentNotifier.test(plan.itemId())) {
					return ActionResult.FAILED;
				}
				return plan.beforeCount() == plan.afterCount() || inventoryPersistence.getAsBoolean()
					? ActionResult.ALREADY_APPLIED : ActionResult.FAILED;
			}
			if (current != plan.beforeCount()) {
				return ActionResult.FAILED;
			}
			boolean changed = switch (plan.kind()) {
				case GIVE_TOP_UP_TO, GIVE_ADD_EXACT -> itemGrant.test(plan.itemId(), plan.afterCount() - plan.beforeCount());
				case REMOVE_EXACT, REMOVE_OPTIONAL_EXACT -> itemRemoval.test(plan.itemId(), plan.requestedCount());
				case REMOVE_ALL -> itemRemoval.test(plan.itemId(), plan.beforeCount());
			};
			if (!changed || itemCountReader.applyAsLong(plan.itemId()) != plan.afterCount()) {
				return ActionResult.FAILED;
			}
			return inventoryPersistence.getAsBoolean() ? ActionResult.APPLIED : ActionResult.FAILED;
		}
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
		return false;
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
