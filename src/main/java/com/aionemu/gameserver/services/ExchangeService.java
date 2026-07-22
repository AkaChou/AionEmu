package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.trade.Exchange;
import com.aionemu.gameserver.model.trade.ExchangeItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DELETE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EXCHANGE_ADD_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EXCHANGE_ADD_KINAH;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EXCHANGE_CONFIRMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EXCHANGE_REQUEST;
import com.aionemu.gameserver.restrictions.RestrictionsManager;
import com.aionemu.gameserver.services.item.ItemFactory;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 玩家交易服务：管理面对面交易会话、物品/基纳挂牌、确认与最终结算落库。
 * Player exchange service: manages face-to-face trade sessions, item/kinah offers, confirmation and final inventory persistence.
 *
 * @author ATracer
 */
@Slf4j(topic = "EXCHANGE_LOG")
public class ExchangeService {


	/** 玩家对象 ID 到交易会话 / Player objectId to exchange session */
	private ConcurrentMap<Integer, Exchange> exchanges = new ConcurrentHashMap<Integer, Exchange>();

	private static volatile ObjectProvider<ExchangeService> instanceProvider;

	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static final ExchangeService getInstance() {
		ObjectProvider<ExchangeService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<ExchangeService> instanceProvider) {
		ExchangeService.instanceProvider = instanceProvider;
	}

	/**
	 * 默认构造：初始化交易存库周期任务。
	 * Default constructor: initializes the periodic exchange save task.
	 */
	public ExchangeService() {
	}

	/**
	 * 在双方通过限制校验后建立交易会话并互发请求包。
	 * Opens an exchange session for both players after restriction checks and sends request packets.
	 *
	 * initiator
	 * partner
	 */
	public void registerExchange(Player player1, Player player2) {
		if (!validateParticipants(player1, player2)) {
			return;
		}
		synchronized (exchanges) {
			player1.setTrading(true);
			player2.setTrading(true);
			exchanges.put(player1.getObjectId(), new Exchange(player1, player2));
			exchanges.put(player2.getObjectId(), new Exchange(player2, player1));
		}

		PacketSendUtility.sendPacket(player2, new SM_EXCHANGE_REQUEST(player1.getName()));
		PacketSendUtility.sendPacket(player1, new SM_EXCHANGE_REQUEST(player2.getName()));
	}

	/**
	 * 校验双方是否允许交易。
	 * Validates that both players are allowed to trade.
	 *
	 * player 1
	 * player 2
	 *
	 * @return 是否可交易 / whether trade is allowed
	 */
	private boolean validateParticipants(Player player1, Player player2) {
		return RestrictionsManager.canTrade(player1) && RestrictionsManager.canTrade(player2);
	}

	/**
	 * 获取当前交易对方。
	 * Returns the current trade partner.
	 *
	 * @param player 玩家 / player
	 * @return 对方玩家，无会话则为 null / partner, or null
	 */
	private Player getCurrentParter(Player player) {
		Exchange exchange = getCurrentExchange(player);
		return exchange != null ? exchange.getTargetPlayer() : null;
	}

	/**
	 * 获取玩家当前交易会话。
	 * Returns the player's current exchange session.
	 *
	 * 玩家 / player
	 * exchange session
	 */
	private Exchange getCurrentExchange(Player player) {
		synchronized (exchanges) {
			return exchanges.get(player.getObjectId());
		}
	}

	/**
	 * 获取对方视角下的交易会话。
	 * Returns the partner's exchange session for this player.
	 *
	 * @param player 玩家 / player
	 * @return 对方交易会话 / partner exchange session
	 */
	public Exchange getCurrentParnterExchange(Player player) {
		Player partner = getCurrentParter(player);
		return partner != null ? getCurrentExchange(partner) : null;
	}

	/**
	 * 判断玩家是否处于交易中。
	 * Returns whether the player is currently in an exchange.
	 *
	 * @param player 玩家 / player
	 * @return 是否交易中 / whether in exchange
	 */
	public boolean isPlayerInExchange(Player player) {
		return getCurrentExchange(player) != null;
	}

	/**
	 * 向交易栏添加基纳并通知双方。
	 * Adds kinah to the exchange offer and notifies both players.
	 *
	 * active player
	 * kinah amount
	 */
	public void addKinah(Player activePlayer, long itemCount) {
		Exchange currentExchange = getCurrentExchange(activePlayer);
		if (currentExchange == null || currentExchange.isLocked()) {
			return;
		}
		if (itemCount < 1) {
			return;
		}
		// 统计背包中总数量 / count total amount in inventory
		long availableCount = activePlayer.getInventory().getKinah();

		// 统计已加入交易的数量 / count amount that was already added to exchange
		availableCount -= currentExchange.getKinahCount();

		long countToAdd = availableCount > itemCount ? itemCount : availableCount;

		if (countToAdd > 0) {
			Player partner = getCurrentParter(activePlayer);
			PacketSendUtility.sendPacket(activePlayer, new SM_EXCHANGE_ADD_KINAH(countToAdd, 0));
			PacketSendUtility.sendPacket(partner, new SM_EXCHANGE_ADD_KINAH(countToAdd, 1));
			currentExchange.addKinah(countToAdd);
		}
	}

	/**
	 * 向交易栏添加物品（含可堆叠增量）并通知双方。
	 * Adds an item to the exchange offer (including stack increments) and notifies both players.
	 *
	 * active player
	 * item object id
	 * count
	 */
	public void addItem(Player activePlayer, int itemObjId, long itemCount) {
		Item item = activePlayer.getInventory().getItemByObjId(itemObjId);
		if (item == null) {
			return;
		}
		Player partner = getCurrentParter(activePlayer);
		if (partner == null) {
			return;
		}
		if (!GameTaskManagerServices.temporaryTradeTimeTask().canTrade(item, partner.getObjectId())) {
			if (!item.isTradeable(activePlayer)) {
				return;
			}
			if (!item.isTradeable(activePlayer)
					&& (item.getWrappableCount() <= item.getItemTemplate().getWrappableCount() && !item.isPacked())) {
				return;
			}
		}

		if (itemCount < 1) {
			return;
		}
		if (itemCount > item.getItemCount()) {
			return;
		}
		Exchange currentExchange = getCurrentExchange(activePlayer);

		if (currentExchange == null) {
			return;
		}
		if (currentExchange.isLocked()) {
			return;
		}
		if (currentExchange.isExchangeListFull()) {
			return;
		}
		if (!GameRuntimeServices.adminService().canOperate(activePlayer, partner, item, "trade")) {
			return;
		}
		ExchangeItem exchangeItem = currentExchange.getItems().get(item.getObjectId());

		long actuallAddCount = 0;
		// 物品先前未添加 / item was not added previosly
		if (exchangeItem == null) {
			Item newItem = null;
			if (itemCount < item.getItemCount()) {
				newItem = ItemFactory.newItem(item.getItemId(), itemCount);
			} else {
				newItem = item;
			}
			exchangeItem = new ExchangeItem(itemObjId, itemCount, newItem);
			currentExchange.addItem(itemObjId, exchangeItem);
			actuallAddCount = itemCount;
		}
		// 物品已添加 / item was already added
		else {
			// 若玩家添加数量超过可能值 / if player add item count that is more than possible
			// 利用漏洞时发生 / happens with exploits
			if (item.getItemCount() == exchangeItem.getItemCount()) {
				return;
			}
			long possibleToAdd = item.getItemCount() - exchangeItem.getItemCount();
			actuallAddCount = itemCount > possibleToAdd ? possibleToAdd : itemCount;
			exchangeItem.addCount(actuallAddCount);
		}

		PacketSendUtility.sendPacket(activePlayer, new SM_EXCHANGE_ADD_ITEM(0, exchangeItem.getItem(), activePlayer));
		PacketSendUtility.sendPacket(partner, new SM_EXCHANGE_ADD_ITEM(1, exchangeItem.getItem(), partner));

		Item exchangedItem = exchangeItem.getItem();
	}

	/**
	 * 锁定己方交易栏并通知对方。
	 * Locks this side of the exchange and notifies the partner.
	 *
	 * active player
	 */
	public void lockExchange(Player activePlayer) {
		Exchange exchange = getCurrentExchange(activePlayer);
		if (exchange != null) {
			exchange.lock();
			Player currentParter = getCurrentParter(activePlayer);
			PacketSendUtility.sendPacket(currentParter, new SM_EXCHANGE_CONFIRMATION(3));
		}
	}

	/**
	 * 取消交易并清理双方会话。
	 * Cancels the exchange and cleans both sessions.
	 *
	 * active player
	 */
	public void cancelExchange(Player activePlayer) {
		Player currentParter = getCurrentParter(activePlayer);
		cleanupExchanges(true, activePlayer, currentParter);
		if (currentParter != null) {
			PacketSendUtility.sendPacket(currentParter, new SM_EXCHANGE_CONFIRMATION(1));
		}
	}

	/**
	 * 确认交易；双方都确认后执行结算。
	 * Confirms this side; when both confirmed, performs the trade.
	 *
	 * active player
	 */
	public void confirmExchange(Player activePlayer) {
		if (activePlayer == null || !activePlayer.isOnline()) {
			return;
		}
		Exchange currentExchange = getCurrentExchange(activePlayer);

		// 取消或登出移除交易后，确认包仍可能到达。 / A confirmation packet may arrive after cancellation or logout removed the exchange.
		if (currentExchange == null) {
			return;
		}
		currentExchange.confirm();

		Player currentPartner = getCurrentParter(activePlayer);
		PacketSendUtility.sendPacket(currentPartner, new SM_EXCHANGE_CONFIRMATION(2));

		if (getCurrentExchange(currentPartner).isConfirmed()) {
			performTrade(activePlayer, currentPartner);
		}
	}

	/**
	 * 执行交易：校验背包、扣物、入包并排队异步存库。
	 * Performs the trade: validates bags, removes items, deposits them, and queues async inventory save.
	 *
	 * one player
	 * partner
	 */
	private void performTrade(Player activePlayer, Player currentPartner) {
		Exchange exchange1 = getCurrentExchange(activePlayer);
		Exchange exchange2 = getCurrentExchange(currentPartner);
		InventorySnapshot activeInventory = InventorySnapshot.capture(activePlayer);
		InventorySnapshot partnerInventory = InventorySnapshot.capture(currentPartner);

		if (!validateExchange(activePlayer, currentPartner)) {
			cleanupExchanges(true, activePlayer, currentPartner);
			return;
		}

		if (!removeItemsFromInventory(activePlayer, exchange1)
				|| !removeItemsFromInventory(currentPartner, exchange2)) {
			activeInventory.restore(activePlayer);
			partnerInventory.restore(currentPartner);
			cleanupExchanges(true, activePlayer, currentPartner);
			AuditLogger.info(activePlayer, "Exchange kinah exploit partner: " + currentPartner.getName());
			return;
		}

		if (!putItemToInventory(currentPartner, exchange1, exchange2)
				|| !putItemToInventory(activePlayer, exchange2, exchange1)) {
			activeInventory.restore(activePlayer);
			partnerInventory.restore(currentPartner);
			cleanupExchanges(true, activePlayer, currentPartner);
			return;
		}

		ExchangeOpSaveTask saveTask = new ExchangeOpSaveTask(exchange1.getActiveplayer().getObjectId(),
				exchange2.getActiveplayer().getObjectId(), exchange1.getItemsToUpdate(), exchange2.getItemsToUpdate());
		if (!saveTask.save()) {
			activeInventory.restore(activePlayer);
			partnerInventory.restore(currentPartner);
			cleanupExchanges(true, activePlayer, currentPartner);
			return;
		}
		PacketSendUtility.sendPacket(activePlayer, new SM_EXCHANGE_CONFIRMATION(0));
		PacketSendUtility.sendPacket(currentPartner, new SM_EXCHANGE_CONFIRMATION(0));
		cleanupExchanges(false, activePlayer, currentPartner);
	}

	/**
	 * 清理交易会话与交易标记，取消或失败时释放临时拆分物品 ID。
	 * Clears exchange sessions and flags, releasing temporary split-item ids on cancel or failure.
	 *
	 * @param releaseIds 是否释放临时物品 ID / whether temporary item ids should be released
	 * exchange players
	 */
	private void cleanupExchanges(boolean releaseIds, Player... players) {
		synchronized (exchanges) {
			for (Player player : players) {
				if (player == null) {
					continue;
				}
				Exchange exchange = exchanges.remove(player.getObjectId());
				player.setTrading(false);
				if (exchange != null && releaseIds) {
					for (ExchangeItem exchangeItem : exchange.getItems().values()) {
						if (exchangeItem.getItemObjId() != exchangeItem.getItem().getObjectId()) {
							ItemService.releaseItemId(exchangeItem.getItem());
						}
					}
				}
			}
		}
	}

	/**
	 * 从背包扣除交易物品与基纳。
	 * Removes offered items and kinah from inventory.
	 *
	 * 玩家 / player
	 * exchange session
	 * whether successful
	 */
	private boolean removeItemsFromInventory(Player player, Exchange exchange) {
		Storage inventory = player.getInventory();

		for (ExchangeItem exchangeItem : exchange.getItems().values()) {
			Item item = exchangeItem.getItem();
			Item itemInInventory = inventory.getItemByObjId(exchangeItem.getItemObjId());
			if (itemInInventory == null) {
				AuditLogger.info(player, "Try to trade unexisting item.");
				return false;
			}

			long itemCount = exchangeItem.getItemCount();

			if (itemCount < itemInInventory.getItemCount()) {
				inventory.decreaseItemCount(itemInInventory, itemCount);
				exchange.addItemToUpdate(itemInInventory);
			} else {
				// 仅从源背包移除 / remove from source inventory only
				inventory.remove(itemInInventory);
				exchangeItem.setItem(itemInInventory);
				// 开始时仅部分堆叠加入→完整堆叠时释放。 / release when only part stack was added in the beginning -> full stack in the
				// 结束 / end
				if (item.getObjectId() != exchangeItem.getItemObjId()) {
					ItemService.releaseItemId(item);
				}
				PacketSendUtility.sendPacket(player, new SM_DELETE_ITEM(itemInInventory.getObjectId()));
			}
		}
		if (!player.getInventory().tryDecreaseKinah(exchange.getKinahCount()))
			return false;
		exchange.addItemToUpdate(player.getInventory().getKinahItem());
		return true;
	}

	/**
	 * 校验双方背包空间是否足以接收对方物品。
	 * Validates both inventories have room for the partner's items.
	 *
	 * one player
	 * partner
	 *
	 * @return 是否通过校验 / whether validation passes
	 */
	private boolean validateExchange(Player activePlayer, Player currentPartner) {
		Exchange exchange1 = getCurrentExchange(activePlayer);
		Exchange exchange2 = getCurrentExchange(currentPartner);

		return validateInventorySize(activePlayer, exchange2) && validateInventorySize(currentPartner, exchange1);
	}

	/**
	 * 校验背包空位是否足够。
	 * Validates free inventory slots are sufficient.
	 *
	 * 玩家 / player
	 * @param exchange 待接收的交易内容 / exchange content to receive
	 * whether enough free slots
	 */
	private boolean validateInventorySize(Player activePlayer, Exchange exchange) {
		int numberOfFreeSlots = activePlayer.getInventory().getFreeSlots();
		return numberOfFreeSlots >= exchange.getItems().size();
	}

	/**
	 * 将对方挂出的物品与基纳放入己方背包。
	 * Deposits the partner's offered items and kinah into this inventory.
	 *
	 * receiver
	 * @param exchange1 对方挂出内容 / partner offer
	 * @param exchange2 己方会话（用于收集待更新物品） / own session (collects items to update)
	 */
	private boolean putItemToInventory(Player player, Exchange exchange1, Exchange exchange2) {
		for (ExchangeItem exchangeItem : exchange1.getItems().values()) {
			Item itemToPut = exchangeItem.getItem();
			itemToPut.setEquipmentSlot(0);
			if (player.getInventory().add(itemToPut) == null) {
				return false;
			}
			exchange2.addItemToUpdate(itemToPut);
		}
		long kinahToExchange = exchange1.getKinahCount();
		if (kinahToExchange > 0) {
			player.getInventory().increaseKinah(exchange1.getKinahCount());
			exchange2.addItemToUpdate(player.getInventory().getKinahItem());
		}
		return true;
	}

	private record ItemSnapshot(Item item, long count, int location, long equipmentSlot, PersistentState state) {}

	private record InventorySnapshot(Storage inventory, PersistentState state, List<ItemSnapshot> items,
			List<Item> deletedItems) {

		private static InventorySnapshot capture(Player player) {
			Storage inventory = player.getInventory();
			List<ItemSnapshot> items = new ArrayList<>();
			for (Item item : inventory.getItemsWithKinah()) {
				items.add(new ItemSnapshot(item, item.getItemCount(), item.getItemLocation(), item.getEquipmentSlot(),
						item.getPersistentState()));
			}
			return new InventorySnapshot(inventory, inventory.getPersistentState(), items,
					new ArrayList<>(inventory.getDeletedItems()));
		}

		private void restore(Player player) {
			for (Item currentItem : new ArrayList<>(inventory.getItems())) {
				if (items.stream().noneMatch(snapshot -> snapshot.item().getObjectId() == currentItem.getObjectId())) {
					inventory.remove(currentItem);
					PacketSendUtility.sendPacket(player, new SM_DELETE_ITEM(currentItem.getObjectId()));
				}
			}
			for (ItemSnapshot snapshot : items) {
				Item item = snapshot.item();
				if (item.getItemTemplate().isKinah()) {
					long difference = snapshot.count() - inventory.getKinah();
					if (difference > 0) {
						inventory.increaseKinah(difference);
					} else if (difference < 0) {
						inventory.decreaseKinah(-difference);
					}
				} else {
					Item currentItem = inventory.getItemByObjId(item.getObjectId());
					if (currentItem == null) {
						item.setItemCount(snapshot.count());
						item.setEquipmentSlot(snapshot.equipmentSlot());
						item.setItemLocation(snapshot.location());
						inventory.add(item);
					} else {
						long difference = snapshot.count() - currentItem.getItemCount();
						if (difference > 0) {
							inventory.increaseItemCount(currentItem, difference);
						} else if (difference < 0) {
							inventory.decreaseItemCount(currentItem, -difference);
						}
					}
				}
				item.setEquipmentSlot(snapshot.equipmentSlot());
				item.setItemLocation(snapshot.location());
				item.setPersistentState(snapshot.state());
			}
			inventory.getDeletedItems().removeIf(item -> !deletedItems.contains(item));
			inventory.setPersistentState(state);
		}
	}

	/**
	 * 将双方交易涉及物品一次性写入数据库。
	 * Persists all items involved in an exchange for both players in one shot.
	 */
	public static final class ExchangeOpSaveTask implements Runnable {

		private int player1Id;
		private int player2Id;
		private List<Item> player1Items;
		private List<Item> player2Items;

		/**
		 * player 1 id
		 * player 2 id
		 * @param player1Items 玩家1 待存物品 / player 1 items to store
		 * @param player2Items 玩家2 待存物品 / player 2 items to store
		 */
		public ExchangeOpSaveTask(int player1Id, int player2Id, List<Item> player1Items, List<Item> player2Items) {
			this.player1Id = player1Id;
			this.player2Id = player2Id;
			this.player1Items = player1Items;
			this.player2Items = player2Items;
		}

		@Override
		public void run() {
			save();
		}

		public boolean save() {
			InventoryDAO inventoryDAO = DAOManager.getDAO(InventoryDAO.class);
			try (Connection con = DatabaseFactory.getConnection()) {
				con.setAutoCommit(false);
				try {
					inventoryDAO.storeInTransaction(con, player1Items, player1Id, null, null);
					inventoryDAO.storeInTransaction(con, player2Items, player2Id, null, null);
					con.commit();
				} catch (SQLException e) {
					con.rollback();
					throw e;
				}
			} catch (SQLException e) {
				log.error(I18n.get("log.39a7be863899", player1Id, player2Id, e), e);
				return false;
			}
			inventoryDAO.markStored(player1Items);
			inventoryDAO.markStored(player2Items);
			return true;
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final ExchangeService instance = new ExchangeService();
	}
}
