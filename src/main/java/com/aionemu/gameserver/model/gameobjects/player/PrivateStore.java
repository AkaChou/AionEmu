package com.aionemu.gameserver.model.gameobjects.player;

import java.util.LinkedHashMap;

import com.aionemu.gameserver.model.trade.TradePSItem;

/**
 * PrivateStore 游戏对象。
 * Private Store game object.
 *
 * @author Xav Modified by Simple
 */
public class PrivateStore {

	private final Player owner;
	private final LinkedHashMap<Integer, TradePSItem> items;
	private String storeMessage;

	/**
	 * 将玩家绑定到商店并创建物品列表。
	 * This method binds a player to the store and creates a list of items.
	 */
	public PrivateStore(Player owner) {
		this.owner = owner;
		this.items = new LinkedHashMap<Integer, TradePSItem>();
	}

	/**
	 * 将 return 所有者。
	 * This method will return the owner of the store
	 *
	 * @return Player
	 */
	public Player getOwner() {
		return owner;
	}

	/**
	 * 将 returnitemsbeingsold。
	 * This method will return the items being sold
	 *
	 * @return LinkedHashMap<Integer, TradePSItem>
	 */
	public synchronized LinkedHashMap<Integer, TradePSItem> getSoldItems() {
		LinkedHashMap<Integer, TradePSItem> snapshot = new LinkedHashMap<Integer, TradePSItem>();
		for (TradePSItem item : items.values()) {
			snapshot.put(item.getItemObjId(), copy(item));
		}
		return snapshot;
	}

	/**
	 * 将物品列表 price。
	 * This method will add an item to the list and price
	 *
	 * @param itemObjId
	 * @param tradeItem
	 */
	public synchronized void addItemToSell(int itemObjId, TradePSItem tradeItem) {
		items.put(itemObjId, copy(tradeItem));
	}

	/**
	 * 将物品列表。
	 * This method will remove an item from the list
	 *
	 * @param itemObjId
	 */
	public synchronized void removeItem(int itemObjId) {
		items.remove(itemObjId);
	}

	/**
	 * @param itemObjId 要查找的物品对象 ID / item object id to look up
	 */
	public synchronized TradePSItem getTradeItemByObjId(int itemObjId) {
		TradePSItem item = items.get(itemObjId);
		return item == null ? null : copy(item);
	}

	/**
	 * 减少上架物品数量，售罄时移除。
	 * Decreases a listed item count and removes it when sold out.
	 */
	public synchronized void decreaseItemCount(int itemObjId, long count) {
		TradePSItem item = items.get(itemObjId);
		item.decreaseCount(count);
		if (item.getCount() <= 0) {
			items.remove(itemObjId);
		}
	}

	/**
	 * @param storeMessage the storeMessage to set
	 */
	public synchronized void setStoreMessage(String storeMessage) {
		this.storeMessage = storeMessage;
	}

	/**
	 * @return the storeMessage
	 */
	public synchronized String getStoreMessage() {
		return storeMessage;
	}

	private static TradePSItem copy(TradePSItem item) {
		return new TradePSItem(item.getItemObjId(), item.getItemId(), item.getCount(), item.getPrice());
	}
}
