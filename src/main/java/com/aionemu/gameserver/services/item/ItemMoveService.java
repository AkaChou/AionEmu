package com.aionemu.gameserver.services.item;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import static com.aionemu.gameserver.services.item.ItemPacketService.sendItemDeletePacket;
import static com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.ItemStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemDeleteType;

/**
 * 物品移动服务，处理仓库间移动与交换。
 * Item move service handling inter-storage moves and swaps.
 *
 * @author ATracer
 */

public class ItemMoveService {

	/**
	 * 移动物品。
	 * Moves an item.
	 *
	 * 玩家 / player
	 * itemObjId
	 * @param sourceStorageType 源仓库类型 / sourceStorageType
	 * @param destinationStorageType 目标仓库类型 / destinationStorageType
	 * slot
	 */
	public static void moveItem(Player player, int itemObjId, byte sourceStorageType, byte destinationStorageType,
			short slot) {
		if (GameRuntimeServices.exchangeService().isPlayerInExchange(player)) {
			return;
		}
		IStorage sourceStorage = player.getStorage(sourceStorageType);
		Item item = player.getStorage(sourceStorageType).getItemByObjId(itemObjId);

		if (item == null) {
			return;
		}
		if (sourceStorageType == destinationStorageType) {
			if (item.getEquipmentSlot() != slot) {
				moveInSameStorage(sourceStorage, item, slot);
			}
			return;
		}

		if (sourceStorageType != destinationStorageType
				&& (ItemRestrictionService.isItemRestrictedTo(player, item, destinationStorageType)
						|| ItemRestrictionService.isItemRestrictedFrom(player, item, sourceStorageType))) {
			sendStorageUpdatePacket(player, StorageType.getStorageTypeById(sourceStorageType), item,
					ItemAddType.ALL_SLOT);
			return;
		}
		IStorage targetStorage = player.getStorage(destinationStorageType);
		GameCoreGameplayServices.legionService().addWHItemHistory(player, item.getItemId(), item.getItemCount(), sourceStorage,
				targetStorage);
		if (slot == -1) {
			if (item.getItemTemplate().isStackable()) {
				List<Item> sameItems = targetStorage.getItemsByItemId(item.getItemId());
				for (Item sameItem : sameItems) {
					long itemCount = item.getItemCount();
					if (itemCount == 0) {
						break;
					}
					// 可合并相同可堆叠物品 / we can merge same stackable items
					ItemSplitService.mergeStacks(sourceStorage, targetStorage, item, sameItem, itemCount);
				}
			}
		}
		if (!targetStorage.isFull() && item.getItemCount() > 0) {
			sourceStorage.remove(item);
			sendItemDeletePacket(player, StorageType.getStorageTypeById(sourceStorageType), item, ItemDeleteType.MOVE);
			item.setEquipmentSlot(
					sourceStorageType == destinationStorageType ? slot : ItemStorage.FIRST_AVAILABLE_SLOT);
			targetStorage.add(item);
		}
	}

	/**
	 * @param storage
	 * @param item
	 * @param slot
	 */
	private static void moveInSameStorage(IStorage storage, Item item, short slot) {
		storage.setPersistentState(PersistentState.UPDATE_REQUIRED);
		item.setEquipmentSlot(slot);
		item.setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * 交换仓库物品。
	 * Switches items between storages.
	 *
	 * @param player 玩家 / player
	 * @param sourceStorageType 源仓库类型 / sourceStorageType
	 * @param sourceItemObjId 源物品对象 ID / sourceItemObjId
	 * @param replaceStorageType 替换仓库类型 / replaceStorageType
	 * @param replaceItemObjId 替换物品对象 ID / replaceItemObjId
	 */
	public static void switchItemsInStorages(Player player, byte sourceStorageType, int sourceItemObjId,
			byte replaceStorageType, int replaceItemObjId) {
		IStorage sourceStorage = player.getStorage(sourceStorageType);
		IStorage replaceStorage = player.getStorage(replaceStorageType);

		Item sourceItem = sourceStorage.getItemByObjId(sourceItemObjId);
		if (sourceItem == null) {
			return;
		}
		Item replaceItem = replaceStorage.getItemByObjId(replaceItemObjId);
		if (replaceItem == null) {
			return;
		}
		// 限制检查 / restrictions checks
		if (ItemRestrictionService.isItemRestrictedFrom(player, sourceItem, sourceStorageType)
				|| ItemRestrictionService.isItemRestrictedFrom(player, replaceItem, replaceStorageType)
				|| ItemRestrictionService.isItemRestrictedTo(player, sourceItem, replaceStorageType)
				|| ItemRestrictionService.isItemRestrictedTo(player, replaceItem, sourceStorageType))
			return;

		long sourceSlot = sourceItem.getEquipmentSlot();
		long replaceSlot = replaceItem.getEquipmentSlot();

		sourceItem.setEquipmentSlot(replaceSlot);
		replaceItem.setEquipmentSlot(sourceSlot);

		sourceStorage.remove(sourceItem);
		replaceStorage.remove(replaceItem);

		// 正确 UI 更新顺序：1）删除物品 2）添加物品 / correct UI update order is 1)delete items 2) add items
		sendItemDeletePacket(player, StorageType.getStorageTypeById(sourceStorageType), sourceItem,
				ItemDeleteType.MOVE);
		sendItemDeletePacket(player, StorageType.getStorageTypeById(replaceStorageType), replaceItem,
				ItemDeleteType.MOVE);
		sourceStorage.add(replaceItem);
		replaceStorage.add(sourceItem);
	}
}