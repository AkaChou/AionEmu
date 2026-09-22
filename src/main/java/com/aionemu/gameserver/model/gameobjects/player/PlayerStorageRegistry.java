package com.aionemu.gameserver.model.gameobjects.player;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.LegionStorageProxy;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;

/**
 * 玩家储物注册表域。
 * Player storage-registry domain.
 * <p>该类型只服务 {@link Player}：负责 CUBE、宠物背包、房屋仓库、普通/账号/军团仓库的
 * 注册、查询，以及脏物品收集与“已存储”标记。字段和 Lombok 访问器仍留在
 * {@link Player}，此处为无状态静态策略。
 * This type only serves {@link Player}: it owns registration and lookup for CUBE, pet bags, house
 * cabinets, regular/account/legion warehouses, dirty-item collection and stored-state marking.
 * Fields and Lombok accessors stay on {@link Player}; this is a stateless static policy.</p>
 */
final class PlayerStorageRegistry {

	private PlayerStorageRegistry() {
	}

	/**
	 * 按类型注册储物容器。
	 * Registers a storage container by type.
	 * @param player 玩家 / player
	 * @param storage 储物容器 / storage
	 * @param storageType 储物类型 / storage type
	 */
	static void setStorage(Player player, Storage storage, StorageType storageType) {
		if (storageType == StorageType.CUBE) {
			player.setInventory(storage);
		}
		if (storageType.getId() >= StorageType.PET_BAG_MIN && storageType.getId() <= StorageType.PET_BAG_MAX) {
			player.getPetBag()[storageType.getId() - StorageType.PET_BAG_MIN] = storage;
		}
		if (storageType.getId() >= StorageType.HOUSE_WH_MIN && storageType.getId() <= StorageType.HOUSE_WH_MAX) {
			player.getCabinets()[storageType.getId() - StorageType.HOUSE_WH_MIN] = storage;
		}
		if (storageType == StorageType.REGULAR_WAREHOUSE) {
			player.setRegularWarehouse(storage);
		}
		if (storageType == StorageType.ACCOUNT_WAREHOUSE) {
			player.setAccountWarehouse(storage);
		}
		storage.setOwner(player);
	}

	/**
	 * 按类型获取储物容器。
	 * Returns the storage container for the given type.
	 * @param player 玩家 / player
	 * @param storageType 储物类型 ID / storage type id
	 * @return 储物容器，未知类型时为 null / storage, or null for unknown types
	 */
	static IStorage getStorage(Player player, int storageType) {
		if (storageType == StorageType.REGULAR_WAREHOUSE.getId()) {
			return player.getRegularWarehouse();
		}

		if (storageType == StorageType.ACCOUNT_WAREHOUSE.getId()) {
			return player.getAccountWarehouse();
		}

		if (storageType == StorageType.LEGION_WAREHOUSE.getId() && player.getLegion() != null) {
			return new LegionStorageProxy(player.getLegion().getLegionWarehouse(), player);
		}

		if (storageType >= StorageType.PET_BAG_MIN && storageType <= StorageType.PET_BAG_MAX) {
			return player.getPetBag()[storageType - StorageType.PET_BAG_MIN];
		}

		if (storageType >= StorageType.HOUSE_WH_MIN && storageType <= StorageType.HOUSE_WH_MAX) {
			return player.getCabinets()[storageType - StorageType.HOUSE_WH_MIN];
		}

		if (storageType == StorageType.CUBE.getId()) {
			return player.getInventory();
		}
		return null;
	}

	/**
	 * 收集需要持久化的脏物品。
	 * Collects dirty items that need persistence.
	 * @param player 玩家 / player
	 * @return 脏物品列表 / dirty items
	 */
	static List<Item> getDirtyItemsToUpdate(Player player) {
		List<Item> dirtyItems = new ArrayList<>();

		IStorage cubeStorage = getStorage(player, StorageType.CUBE.getId());
		if (cubeStorage.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
			dirtyItems.addAll(cubeStorage.getItemsWithKinah());
			dirtyItems.addAll(cubeStorage.getDeletedItems());
		}

		IStorage regularWhStorage = getStorage(player, StorageType.REGULAR_WAREHOUSE.getId());
		if (regularWhStorage.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
			dirtyItems.addAll(regularWhStorage.getItemsWithKinah());
			dirtyItems.addAll(regularWhStorage.getDeletedItems());
		}

		IStorage accountWhStorage = getStorage(player, StorageType.ACCOUNT_WAREHOUSE.getId());
		if (accountWhStorage.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
			dirtyItems.addAll(accountWhStorage.getItemsWithKinah());
			dirtyItems.addAll(accountWhStorage.getDeletedItems());
		}

		IStorage legionWhStorage = getStorage(player, StorageType.LEGION_WAREHOUSE.getId());
		if (legionWhStorage != null && legionWhStorage.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
			dirtyItems.addAll(legionWhStorage.getItemsWithKinah());
			dirtyItems.addAll(legionWhStorage.getDeletedItems());
		}

		for (int petBagId = StorageType.PET_BAG_MIN; petBagId <= StorageType.PET_BAG_MAX; petBagId++) {
			IStorage petBag = getStorage(player, petBagId);
			if (petBag != null && petBag.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
				dirtyItems.addAll(petBag.getItemsWithKinah());
				dirtyItems.addAll(petBag.getDeletedItems());
			}
		}

		for (int houseWhId = StorageType.HOUSE_WH_MIN; houseWhId <= StorageType.HOUSE_WH_MAX; houseWhId++) {
			IStorage cabinet = getStorage(player, houseWhId);
			if (cabinet != null && cabinet.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
				dirtyItems.addAll(cabinet.getItemsWithKinah());
				dirtyItems.addAll(cabinet.getDeletedItems());
			}
		}

		Equipment equipment = player.getEquipment();
		if (equipment.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
			dirtyItems.addAll(equipment.getEquippedItems());
		}

		return dirtyItems;
	}

	/**
	 * 将所有已持久化容器标记为 UPDATED。
	 * Marks all persisted containers as UPDATED.
	 * @param player 玩家 / player
	 */
	static void markDirtyItemContainersStored(Player player) {
		for (int storageId : new int[] { StorageType.CUBE.getId(), StorageType.REGULAR_WAREHOUSE.getId(),
				StorageType.ACCOUNT_WAREHOUSE.getId(), StorageType.LEGION_WAREHOUSE.getId() }) {
			IStorage storage = getStorage(player, storageId);
			if (storage != null) {
				storage.setPersistentState(PersistentState.UPDATED);
			}
		}
		for (int storageId = StorageType.PET_BAG_MIN; storageId <= StorageType.PET_BAG_MAX; storageId++) {
			IStorage storage = getStorage(player, storageId);
			if (storage != null) {
				storage.setPersistentState(PersistentState.UPDATED);
			}
		}
		for (int storageId = StorageType.HOUSE_WH_MIN; storageId <= StorageType.HOUSE_WH_MAX; storageId++) {
			IStorage storage = getStorage(player, storageId);
			if (storage != null) {
				storage.setPersistentState(PersistentState.UPDATED);
			}
		}
		player.getEquipment().setPersistentState(PersistentState.UPDATED);
	}

	/**
	 * 返回玩家全部储物与装备中的物品。
	 * Returns all items from player-owned storages and equipment.
	 * @param player 玩家 / player
	 * @return 全部物品 / all items
	 */
	static List<Item> getAllItems(Player player) {
		List<Item> items = new ArrayList<>();
		items.addAll(player.getInventory().getItemsWithKinah());
		if (player.getRegularWarehouse() != null) {
			items.addAll(player.getRegularWarehouse().getItemsWithKinah());
		}
		if (player.getAccountWarehouse() != null) {
			items.addAll(player.getAccountWarehouse().getItemsWithKinah());
		}

		for (int petBagId = StorageType.PET_BAG_MIN; petBagId <= StorageType.PET_BAG_MAX; petBagId++) {
			IStorage petBag = getStorage(player, petBagId);
			if (petBag != null) {
				items.addAll(petBag.getItemsWithKinah());
			}
		}

		for (int houseWhId = StorageType.HOUSE_WH_MIN; houseWhId <= StorageType.HOUSE_WH_MAX; houseWhId++) {
			IStorage cabinet = getStorage(player, houseWhId);
			if (cabinet != null) {
				items.addAll(cabinet.getItemsWithKinah());
			}
		}
		items.addAll(player.getEquipment().getEquippedItems());
		return items;
	}
}
