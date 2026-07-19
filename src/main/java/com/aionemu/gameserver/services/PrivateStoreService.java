package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.aionemu.commons.objects.filter.ObjectFilter;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PrivateStore;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.trade.TradeItem;
import com.aionemu.gameserver.model.trade.TradeList;
import com.aionemu.gameserver.model.trade.TradePSItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PRIVATE_STORE_NAME;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 个人商店服务，处理开店、上架、购买与关店逻辑。
 * Private-store service handling open, list, buy, and close operations.
 *
 * @author Simple
 */
@Slf4j(topic = "EXCHANGE_LOG")
public class PrivateStoreService {


	/**
	 * 成交商店物品：校验双方、转移道具与基纳，售罄时自动关店。
	 * Completes a store sale: validates parties, transfers items and kinah, and closes the store when empty.
	 *
	 * @param seller 卖家 / seller
	 * @param buyer 买家 / buyer
	 * @param tradeList 交易列表 / trade list
	 */
	public static void sellStoreItem(Player seller, Player buyer, TradeList tradeList) {
		if (seller == null) {
			return;
		}
		synchronized (seller) {
			/**
			 * 1. 校验双方参与者是否有效且可交易。
			 * 1. Check if we are busy with two valid participants
			 */
			if (!validateParticipants(seller, buyer))
				return;

			/**
	 * 定义商店变量以简化逻辑 / Define store to make life easier
	 */
			PrivateStore store = seller.getStore();
			if (store == null) {
				return;
			}

			/**
			 * 2. 加载物品对象 ID 并校验卖家是否真正持有。
			 * 2. Load all item object ids and validate if seller really owns them
			 */
			tradeList = loadObjIds(seller, tradeList);
			if (tradeList == null)
				return; // Invalid items found or store was empty

			/**
	 * 3. 检查空闲槽位 / 3. Check free slots
	 */
			Map<Integer, Long> requestedItems = new HashMap<>();
			Map<Integer, ItemTemplate> templates = new HashMap<>();
			try {
				for (TradeItem tradeItem : tradeList.getTradeItems()) {
					Item item = getItemByObjId(seller, tradeItem.getItemId());
					requestedItems.merge(item.getItemId(), tradeItem.getCount(), Math::addExact);
					templates.put(item.getItemId(), item.getItemTemplate());
				}
			} catch (ArithmeticException e) {
				return;
			}
			Storage inventory = buyer.getInventory();
			if (!ItemService.canAddItems(inventory, requestedItems, templates)) {
				PacketSendUtility.sendPacket(buyer, SM_SYSTEM_MESSAGE.STR_MSG_DICE_INVEN_ERROR);
				return;
			}

			/**
	 * 创建 total 价格并物品。 / Create total price and items
	 */
			long price = getTotalPrice(store, tradeList);

			// 基纳漏洞修复 / Kinah exploit fix
			if (price < 0)
				return;

			/**
	 * 检查玩家是否有足够基纳。 / Check whether the player has enough Kinah.
	 */
			if (buyer.getInventory().getKinah() >= price) {
				for (TradeItem tradeItem : tradeList.getTradeItems()) {
					Item item = getItemByObjId(seller, tradeItem.getItemId());
					decreaseItemFromPlayer(seller, item, tradeItem);
					if (ItemService.addItem(buyer, item.getItemId(), tradeItem.getCount(), item) != 0) {
						return;
					}

					// 记录交易 / Log the trade
					log.info(I18n.get("log.d2e08279f4c2", seller.getName(), item.getItemId(), item.getItemCount(), buyer.getName(), price));
				}
				// 减少买家基纳，增加卖家基纳 / Decrease kinah for buyer and Increase kinah for seller
				decreaseKinahAmount(buyer, price);
				increaseKinahAmount(seller, price);

				/**
		 * 从商店移除物品，并检查是否为最后一件。
		 * Remove item from store and check if last item
				 */
				if (store.getSoldItems().size() == 0) {
					closePrivateStore(seller);
				}
			}
		}
	}

	/**
	 * 打开/更新个人商店店名广播；可按阵营过滤可见性。
	 * Opens or updates the private-store name broadcast; may filter visibility by faction.
	 *
	 * store owner
	 * @param name 店名；null 表示清空 / store name; null clears it
	 */
	public static void openPrivateStore(Player activePlayer, String name) {
		synchronized (activePlayer) {
			final int senderRace = activePlayer.getRace().getRaceId();
			final Player playerActive = activePlayer;
			if (name != null) {
				PrivateStore store = activePlayer.getStore();
				if (store == null) {
					return;
				}
				store.setStoreMessage(name);
				if (CustomConfig.SPEAKING_BETWEEN_FACTIONS) {
					PacketSendUtility.broadcastPacket(playerActive,
							new SM_PRIVATE_STORE_NAME(playerActive.getObjectId(), name), true);
				} else {
					PacketSendUtility.broadcastPacket(playerActive,
							new SM_PRIVATE_STORE_NAME(playerActive.getObjectId(), name), true, new ObjectFilter<Player>() {

								@Override
								public boolean acceptObject(Player object) {
									return ((senderRace == object.getRace().getRaceId()
											&& !object.getBlockList().contains(playerActive.getObjectId()))
											|| object.isGM());
								}
							});
					PacketSendUtility.broadcastPacket(playerActive,
							new SM_PRIVATE_STORE_NAME(playerActive.getObjectId(), ""), false, new ObjectFilter<Player>() {

								@Override
								public boolean acceptObject(Player object) {
									return senderRace != object.getRace().getRaceId()
											&& !object.getBlockList().contains(playerActive.getObjectId())
											&& !object.isGM();
								}
							});
				}
			} else {
				PacketSendUtility.broadcastPacket(playerActive, new SM_PRIVATE_STORE_NAME(playerActive.getObjectId(), ""),
						true);
			}
		}
	}

	/**
	 * 将可交易道具加入玩家个人商店。
	 * Adds tradeable items to the player's private store.
	 *
	 * store owner
	 * items to list
	 */
	public static void addItems(Player activePlayer, TradePSItem[] tradePSItems) {
		synchronized (activePlayer) {
			if (CreatureState.ACTIVE.getId() != activePlayer.getState()) {
				return;
			}

			/**
	 * 检查玩家是否已有商店，没有则创建。 / Check whether the player already has a store and create one if needed.
	 */
			if (activePlayer.getStore() == null) {
				createStore(activePlayer);
			}

			PrivateStore store = activePlayer.getStore();
			if (store == null) {
				return;
			}

			/**
	 * 检查玩家是否拥有指定物品，否则不添加。 / Check whether the player owns the item before adding it.
	 */
			for (int i = 0; i < tradePSItems.length; i++) {
				Item item = getItemByObjId(activePlayer, tradePSItems[i].getItemObjId());
				if (item != null && item.isTradeable(activePlayer)) {
					if (validateItem(store, item, tradePSItems[i])) {
						store.addItemToSell(tradePSItems[i].getItemObjId(), tradePSItems[i]);
					}
				}
			}
		}
	}

	/**
	 * 校验上架条目与背包道具是否匹配且未重复。
	 * Validates that the listed entry matches the inventory item and is not already listed.
	 *
	 * store
	 * @param item 背包道具 / inventory item
	 * store listing entry
	 * whether valid
	 */
	private static boolean validateItem(PrivateStore store, Item item, TradePSItem psItem) {
		int itemId = psItem.getItemId();
		long itemCount = psItem.getCount();
		if (item.getItemTemplate().getTemplateId() != itemId)
			return false;
		if (itemCount > item.getItemCount() || itemCount < 1)
			return false;

		TradePSItem addedPsItem = store.getTradeItemByObjId(psItem.getItemObjId());
		return addedPsItem == null;

	}

	/**
	 * 创建玩家个人商店并广播开店表情。
	 * Creates the player's private store and broadcasts the open-shop emotion.
	 *
	 * store owner
	 */
	private static void createStore(Player activePlayer) {
		if (activePlayer.isInState(CreatureState.RESTING)) {
			return;
		}
		activePlayer.setStore(new PrivateStore(activePlayer));
		activePlayer.setState(CreatureState.PRIVATE_SHOP);
		PacketSendUtility.broadcastPacket(activePlayer,
				new SM_EMOTION(activePlayer, EmotionType.OPEN_PRIVATESHOP, 0, 0), true);
	}

	/**
	 * 关闭玩家个人商店并广播关店表情。
	 * Closes the player's private store and broadcasts the close-shop emotion.
	 *
	 * store owner
	 */
	public static void closePrivateStore(Player activePlayer) {
		synchronized (activePlayer) {
			if (activePlayer.getStore() == null) {
				return;
			}
			activePlayer.setStore(null);
			activePlayer.unsetState(CreatureState.PRIVATE_SHOP);
			PacketSendUtility.broadcastPacket(activePlayer,
					new SM_EMOTION(activePlayer, EmotionType.CLOSE_PRIVATESHOP, 0, 0), true);
		}
	}

	/**
	 * 从卖家背包与商店条目中扣减道具数量。
	 * Decreases item count from the seller inventory and store listing.
	 *
	 * 卖家 / seller
	 * item
	 * trade item
	 */
	private static void decreaseItemFromPlayer(Player seller, Item item, TradeItem tradeItem) {
		seller.getInventory().decreaseItemCount(item, tradeItem.getCount());
		seller.getStore().decreaseItemCount(item.getObjectId(), tradeItem.getCount());
	}

	/**
	 * 增加玩家基纳。
	 * Increases the player's kinah.
	 *
	 * target player
	 * amount
	 */
	private static void increaseKinahAmount(Player player, long price) {
		player.getInventory().increaseKinah(price);
	}

	/**
	 * 按对象 ID 从背包取道具。
	 * Returns an inventory item by object id.
	 *
	 * owner
	 * object id
	 * item
	 */
	private static Item getItemByObjId(Player seller, int itemObjId) {
		return seller.getInventory().getItemByObjId(itemObjId);
	}

	/**
	 * 计算交易列表总价。
	 * Calculates the total price of the trade list.
	 *
	 * store
	 * 交易列表 / trade list
	 * total price
	 */
	static long getTotalPrice(PrivateStore store, TradeList tradeList) {
		long totalprice = 0;
		try {
			for (TradeItem tradeItem : tradeList.getTradeItems()) {
				TradePSItem item = store.getTradeItemByObjId(tradeItem.getItemId());
				if (item == null) {
					return -1;
				}
				totalprice = Math.addExact(totalprice, Math.multiplyExact(item.getPrice(), tradeItem.getCount()));
			}
		} catch (ArithmeticException e) {
			return -1;
		}
		return totalprice;
	}

	/**
	 * 将客户端交易索引解析为真实对象 ID 列表。
	 * Resolves client trade indexes into a list of real item object ids.
	 *
	 * @param seller 卖家 / seller
	 * @param tradeList 原始交易列表 / original trade list
	 * @return 新交易列表；校验失败返回 null / new trade list; null if validation fails
	 */
	private static TradeList loadObjIds(Player seller, TradeList tradeList) {
		PrivateStore store = seller.getStore();
		TradeList newTradeList = new TradeList();

		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			int i = 0;
			for (int itemObjId : store.getSoldItems().keySet()) {
				if (i == tradeItem.getItemId()) {
					newTradeList.addPSItem(itemObjId, tradeItem.getCount());
				}
				i++;
			}
		}
		if (newTradeList.size() != tradeList.size()) {
			return null;
		}

		/**
	 * 检查玩家是否仍拥有物品。 / Check whether the player still owns the items.
	 */
		if (!validateBuyItems(seller, newTradeList)) {
			return null;
		}

		return newTradeList;
	}

	/**
	 * 校验买卖双方是否在线且同阵营。
	 * Validates that both parties are online and of the same race.
	 *
	 * 卖家 / seller
	 * 买家 / buyer
	 * whether valid
	 */
	private static boolean validateParticipants(Player itemOwner, Player newOwner) {
		return itemOwner != null && newOwner != null && itemOwner.isOnline() && newOwner.isOnline()
				&& itemOwner.getRace().equals(newOwner.getRace());
	}

	/**
	 * 校验买家购买的道具仍由卖家持有。
	 * Validates that purchased items are still owned by the seller.
	 *
	 * 卖家 / seller
	 * 交易列表 / trade list
	 * whether valid
	 */
	private static boolean validateBuyItems(Player seller, TradeList tradeList) {
		PrivateStore store = seller.getStore();
		Set<Integer> itemObjectIds = new HashSet<Integer>();
		for (TradeItem tradeItem : tradeList.getTradeItems()) {
			Item item = seller.getInventory().getItemByObjId(tradeItem.getItemId());
			TradePSItem storeItem = store.getTradeItemByObjId(tradeItem.getItemId());
			if (!itemObjectIds.add(tradeItem.getItemId()) || item == null || storeItem == null
					|| tradeItem.getCount() < 1 || storeItem.getCount() < tradeItem.getCount()
					|| item.getItemCount() < tradeItem.getCount()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 扣减玩家基纳。
	 * Decreases the player's kinah.
	 *
	 * target player
	 * amount
	 */
	private static void decreaseKinahAmount(Player player, long price) {
		player.getInventory().decreaseKinah(price);
	}
}
