package com.aionemu.gameserver.persistence.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.persistence.entity.InventoryPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家背包与装备数据访问对象。
 * Player inventory and equipment data access object.
 * @author ATracer
 */
public interface InventoryDao extends IService<InventoryPo> {

	/**
	 * 加载指定类型的仓库。
	 * Loads storage of the given type.
	 * @param playerId 玩家 ID / player ID
	 * @param storageType 存储类型 / storage type
	 * @return 存储 / storage
	 */
	Storage loadStorage(int playerId, StorageType storageType);

	/**
	 * 直接加载指定类型仓库中的物品列表。
	 * Loads items of the given storage type directly.
	 * @param playerId 玩家 ID / player ID
	 * @param storageType 存储类型 / storage type
	 * @return 物品列表 / item list
	 */
	List<Item> loadStorageDirect(int playerId, StorageType storageType);

	/**
	 * 加载玩家装备。
	 * Loads player equipment.
	 * @param player 玩家 / player
	 * @return 装备 / equipment
	 */
	Equipment loadEquipment(Player player);

	/**
	 * 按玩家 ID 加载装备物品列表。
	 * Loads equipment items by player ID.
	 * @param playerId 玩家 ID / player ID
	 * @return 物品列表 / item list
	 */
	List<Item> loadEquipment(int playerId);

	/**
	 * 存储玩家的全部物品数据。
	 * Stores all item data for a player.
	 * @param player 玩家 / player
	 * @return 是否成功 / whether successful
	 */
	boolean store(Player player);

	/**
	 * 存储玩家的单个物品。
	 * Stores a single item for a player.
	 * @param item 物品 / item
	 * @param player 玩家 / player
	 * @return 是否成功 / whether successful
	 */
	boolean store(Item item, Player player);

	/**
	 * 按玩家 ID 存储单个物品。
	 * Stores a single item by player ID.
	 * @param item 物品 / item
	 * @param playerId 玩家 ID / player ID
	 * @return 是否成功 / whether successful
	 */
	default boolean store(Item item, int playerId) {
		return store(Collections.singletonList(item), playerId);
	}

	/**
	 * 按玩家 ID 批量存储物品。
	 * Stores a list of items by player ID.
	 * @param items 物品列表 / item list
	 * @param playerId 玩家 ID / player ID
	 * @return 是否成功 / whether successful
	 */
	boolean store(List<Item> items, int playerId);

	/**
	 * 按玩家/账号/军团 ID 存储单个物品。
	 * Stores a single item with player/account/legion IDs.
	 * @param item 物品 / item
	 * @param playerId 玩家 ID / player ID
	 * @param accountId 账号 ID / account ID
	 * @param legionId 军团 ID / legion ID
	 * @return 是否成功 / whether successful
	 */
	default boolean store(Item item, Integer playerId, Integer accountId, Integer legionId) {
		List<Item> temp = new ArrayList<>();
		temp.add(item);
		return store(temp, playerId, accountId, legionId);
	}

	/**
	 * 按玩家/账号/军团 ID 批量存储物品。
	 * Stores a list of items with player/account/legion IDs.
	 * @param items 物品列表 / item list
	 * @param playerId 玩家 ID / player ID
	 * @param accountId 账号 ID / account ID
	 * @param legionId 军团 ID / legion ID
	 * @return 是否成功 / whether successful
	 */
	boolean store(List<Item> items, Integer playerId, Integer accountId, Integer legionId);

	/**
	 * 在调用方事务内按持久化状态写入物品。
	 * Persists items by persistent state inside the caller-owned transaction.
	 * @param items 物品列表 / item list
	 * @param playerId 玩家 ID / player ID
	 * @param accountId 账号 ID / account ID
	 * @param legionId 军团 ID / legion ID
	 */
	void storeInTransaction(List<Item> items, Integer playerId, Integer accountId, Integer legionId);

	/**
	 * 将物品标记为已持久化（删除态物品同时释放其唯一 ID）。
	 * Marks items as stored (deleted items also release their unique id).
	 * @param items 物品集合 / item collection
	 */
	void markStored(Collection<Item> items);

	/**
	 * 删除玩家的全部物品。
	 * Deletes all items for a player.
	 * @param playerId 玩家 ID / player ID
	 * @return 是否成功 / whether successful
	 */
	boolean deletePlayerItems(int playerId);

	/**
	 * 删除账号仓库。
	 * Deletes account warehouse items.
	 * @param accountId 账号 ID / account ID
	 */
	void deleteAccountWH(int accountId);

	/**
	 * 返回本 DAO 已使用的全部 ID。
	 * Returns an array of all IDs that are used by this DAO.
	 * @return 已使用 ID 数组 / array of used IDs
	 */
	int[] getUsedIDs();
}
