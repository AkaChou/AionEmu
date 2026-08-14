package com.aionemu.gameserver.services;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.trade.RepurchaseList;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * 回购服务，缓存玩家卖出物品并支持从商店回购。
 * Repurchase service that caches sold items and allows buying them back from the shop.
 *
 * @author xTz
 */
public class RepurchaseService {

	private static volatile ObjectProvider<RepurchaseService> instanceProvider;
	private Multimap<Integer, Item> repurchaseItems;

	/**
	 * 构造服务并初始化回购缓存。
	 * Constructs the service and initializes the repurchase cache.
	 */
	public RepurchaseService() {
		repurchaseItems = ArrayListMultimap.create();
	}

	/**
	 * 为玩家登记可回购物品。
	 * Registers items available for repurchase for this player.
	 *
	 * @param player 玩家 / player
	 * @param items 物品列表 / item list
	 */
	public void addRepurchaseItems(Player player, List<Item> items) {
		repurchaseItems.putAll(player.getObjectId(), items);
	}

	/**
	 * 清除该玩家全部可回购物品。
	 * Removes all repurchase items for this player.
	 *
	 * @param player 玩家 / player
	 */
	public void removeRepurchaseItems(Player player) {
		repurchaseItems.removeAll(player.getObjectId());
	}

	/**
	 * 移除玩家的单个可回购物品。
	 * Removes a single repurchase item for the player.
	 *
	 * @param player 玩家 / player
	 * @param item 要移除的物品 / item
	 */
	public void removeRepurchaseItem(Player player, Item item) {
		repurchaseItems.get(player.getObjectId()).remove(item);
	}

	/**
	 * 获取玩家当前可回购物品集合。
	 * Returns the current repurchase item collection for the player.
	 *
	 * player object id
	 *
	 * @param playerObjectId
	 * @return 可回购物品；无则空集合 / repurchase items, or empty if none
	 */
	public Collection<Item> getRepurchaseItems(int playerObjectId) {
		Collection<Item> items = repurchaseItems.get(playerObjectId);
		return items != null ? items : Collections.<Item>emptyList();
	}

	/**
	 * 按物品 objectId 查找可回购物品。
	 * Finds a repurchase item by item object id.
	 *
	 * 玩家 / player
	 * item object id
	 * @return 匹配物品，未找到返回 null / matching item, or null if not found
	 */
	public Item getRepurchaseItem(Player player, int itemObjectId) {
		Collection<Item> items = getRepurchaseItems(player.getObjectId());
		for (Item item : items) {
			if (item.getObjectId() == itemObjectId) {
				return item;
			}
		}
		return null;
	}

	/**
	 * 按回购列表从商店回购物品，扣基纳并写审计日志。
	 * Repurchases items from the shop per list, deducts kinah, and audits abuse.
	 *
	 * 玩家 / player
	 * repurchase list
	 */
	public void repurchaseFromShop(Player player, RepurchaseList repurchaseList) {
		Storage inventory = player.getInventory();
		for (Item repurchaseItem : repurchaseList.getRepurchaseItems()) {
			Collection<Item> items = repurchaseItems.get(player.getObjectId());
			if (items != null && items.contains(repurchaseItem)) {
				if (!ItemService.canAddItem(player, repurchaseItem.getItemId(), repurchaseItem.getItemCount())) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_FULL_INVENTORY);
					return;
				}
				if (inventory.tryDecreaseKinah(repurchaseItem.getRepurchasePrice())) {
					if (ItemService.addItem(player, repurchaseItem) == 0) {
						removeRepurchaseItem(player, repurchaseItem);
					} else {
						inventory.increaseKinah(repurchaseItem.getRepurchasePrice());
						return;
					}
				} else {
					AuditLogger.info(player, "Player try repurchase item: " + repurchaseItem.getItemId() + " count: "
							+ repurchaseItem.getItemCount() + " whithout kinah");
				}
			} else {
				AuditLogger.info(player, "Player might be abusing CM_BUY_ITEM try dupe item: "
						+ repurchaseItem.getItemId() + " count: " + repurchaseItem.getItemCount());
			}
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 */
	public static RepurchaseService getInstance() {
		ObjectProvider<RepurchaseService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<RepurchaseService> provider) {
		instanceProvider = provider;
	}

	private static class SingletonHolder {

		protected static final RepurchaseService INSTANCE = new RepurchaseService();
	}
}
