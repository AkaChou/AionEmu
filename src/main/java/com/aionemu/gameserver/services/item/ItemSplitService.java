package com.aionemu.gameserver.services.item;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import static com.aionemu.gameserver.services.item.ItemPacketService.sendStorageUpdatePacket;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.IStorage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CUBE_UPDATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.ExchangeService;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 物品拆分服务，处理堆叠拆分与合并。
 * Item split service handling stack split and merge.
 *
 * @author ATracer
 */

@Slf4j
public class ItemSplitService {


	/**
	 * Move part of stack into different slot
	 */
	public static final void splitItem(Player player, int itemObjId, int destinationObjId, long splitAmount,
			short slotNum, byte sourceStorageType, byte destinationStorageType) {
		if (splitAmount <= 0) {
			return;
		}
		if (player.isTrading()) {
			// 交易中无法在背包内拆分物品。 / You cannot split items in the inventory during a trade.
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300713));
			return;
		}

		IStorage sourceStorage = player.getStorage(sourceStorageType);
		IStorage destStorage = player.getStorage(destinationStorageType);
		if (sourceStorage == null || destStorage == null) {
			log.warn(I18n.get("log.cd5d725c3251", player.getName(), sourceStorageType, destinationStorageType));
			return;
		}
		Item sourceItem = sourceStorage.getItemByObjId(itemObjId);
		Item targetItem = destStorage.getItemByObjId(destinationObjId);

		if (sourceItem == null) {
			sourceItem = sourceStorage.getKinahItem();
			if (sourceItem == null || sourceItem.getObjectId() != itemObjId) {
			log.warn(I18n.get("log.44a3d6f1e0aa", itemObjId, splitAmount, slotNum));
				return;
			}
		}

		if (sourceStorageType != destinationStorageType
				&& (ItemRestrictionService.isItemRestrictedTo(player, sourceItem, destinationStorageType)
						|| ItemRestrictionService.isItemRestrictedFrom(player, sourceItem, sourceStorageType))) {
			sendStorageUpdatePacket(player, StorageType.getStorageTypeById(sourceStorageType), sourceItem);
			return;
		}

		// 在背包与仓库之间转移基纳时客户端使用拆分。 / To move kinah from inventory to warehouse and vice versa client using split
		// 物品数据包 / item packet
		if (sourceItem.getItemTemplate().isKinah()) {
			moveKinah(player, sourceStorage, splitAmount);
			return;
		}

		if (targetItem == null) {
			long oldItemCount = sourceItem.getItemCount() - splitAmount;
			if (sourceItem.getItemCount() < splitAmount || oldItemCount == 0) {
				return;
			}
			if (sourceStorageType != destinationStorageType) {
				GameCoreGameplayServices.legionService().addWHItemHistory(player, sourceItem.getItemId(), splitAmount, sourceStorage,
						destStorage);
			}
			Item newItem = ItemFactory.newItem(sourceItem.getItemTemplate().getTemplateId(), splitAmount);
			if (sourceStorageType == destinationStorageType)
				newItem.setEquipmentSlot(slotNum);
			sourceStorage.decreaseItemCount(sourceItem, splitAmount,
					sourceStorageType == destinationStorageType ? ItemUpdateType.DEC_ITEM_SPLIT
							: ItemUpdateType.DEC_ITEM_SPLIT_MOVE);
			PacketSendUtility.sendPacket(player, SM_CUBE_UPDATE.cubeSize(sourceStorage.getStorageType(), player));
			if (destStorage.add(newItem) == null) {
				// 若物品未添加——可释放其 ID / if item was not added - we can release its id
				ItemService.releaseItemId(newItem);
			}
		} else if (targetItem.getItemId() == sourceItem.getItemId()) {
			if (sourceStorageType != destinationStorageType) {
				GameCoreGameplayServices.legionService().addWHItemHistory(player, sourceItem.getItemId(), splitAmount, sourceStorage,
						destStorage);
			}
			mergeStacks(sourceStorage, destStorage, sourceItem, targetItem, splitAmount);
		}
	}

	/**
	 * Merge 2 stacks with simple validation
	 */
	public static void mergeStacks(IStorage sourceStorage, IStorage destStorage, Item sourceItem, Item targetItem,
			long count) {
		if (sourceItem.getItemCount() >= count) {
			long freeCount = targetItem.getFreeCount();
			count = count > freeCount ? freeCount : count;
			long leftCount = destStorage.increaseItemCount(targetItem, count,
					sourceStorage.getStorageType() == destStorage.getStorageType() ? ItemUpdateType.INC_ITEM_MERGE
							: ItemUpdateType.INC_ITEM_COLLECT);
			sourceStorage.decreaseItemCount(sourceItem, count - leftCount,
					sourceStorage.getStorageType() == destStorage.getStorageType() ? ItemUpdateType.DEC_ITEM_SPLIT
							: ItemUpdateType.DEC_ITEM_SPLIT_MOVE);
		}
	}

	private static void moveKinah(Player player, IStorage source, long splitAmount) {
		if (source.getKinah() < splitAmount) {
			return;
		}
		if (GameRuntimeServices.exchangeService().isPlayerInExchange(player)) {
			return;
		}
		switch (source.getStorageType()) {
		case CUBE: {
			IStorage destination = player.getStorage(StorageType.ACCOUNT_WAREHOUSE.getId());
			long chksum = (source.getKinah() - splitAmount) + (destination.getKinah() + splitAmount);

			if (chksum != source.getKinah() + destination.getKinah()) {
				return;
			}
			updateKinahCount(source, splitAmount, destination);
			break;
		}

		case ACCOUNT_WAREHOUSE: {
			IStorage destination = player.getStorage(StorageType.CUBE.getId());
			long chksum = (source.getKinah() - splitAmount) + (destination.getKinah() + splitAmount);

			if (chksum != source.getKinah() + destination.getKinah()) {
				return;
			}
			updateKinahCount(source, splitAmount, destination);
			break;
		}
		default:
			break;
		}
	}

	private static final void updateKinahCount(IStorage source, long splitAmount, IStorage destination) {
		source.decreaseKinah(splitAmount, ItemUpdateType.DEC_ITEM_SPLIT);
		destination.increaseKinah(splitAmount, ItemUpdateType.INC_KINAH_MERGE);
	}
}
