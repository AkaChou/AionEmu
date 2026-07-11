package com.aionemu.gameserver.services.item;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.configs.main.LegionConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.LegionPermissionsMask;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 物品限制服务，校验物品是否可移除/操作。
 * Item restriction service validating whether items may be removed/operated.
 *
 * @author ATracer
 */
public class ItemRestrictionService {

	/**
	 * 检查玩家是否可从指定仓库移出物品。
	 * Checks whether the player may move the item out of the given storage.
	 *
	 * 玩家 / player
	 * item
	 * storage type id
	 *
	 * @return 受限（不可移出）则为 true / true if restricted (cannot remove)
	 */
	public static boolean isItemRestrictedFrom(Player player, Item item, byte storage) {
		StorageType type = StorageType.getStorageTypeById(storage);
		switch (type) {
		case LEGION_WAREHOUSE:
			if (!GameCoreGameplayServices.legionService().getLegionMember(player.getObjectId())
					.hasRights(LegionPermissionsMask.WH_WITHDRAWAL) || !LegionConfig.LEGION_WAREHOUSE
					|| !player.isLegionMember()) {
				// 无权使用军团仓库。 / No authority to use legion warehouse.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300322));
				return true;
			}
			break;
		}
		return false;
	}

	/**
	 * 检查玩家是否可将物品移入指定仓库。
	 * Checks whether the player may move the item into the given storage.
	 *
	 * 玩家 / player
	 * item
	 * storage type id
	 *
	 * @return 受限（不可移入）则为 true / true if restricted (cannot deposit)
	 */
	public static boolean isItemRestrictedTo(Player player, Item item, byte storage) {
		StorageType type = StorageType.getStorageTypeById(storage);
		switch (type) {
		case REGULAR_WAREHOUSE:
			if (!item.isStorableinWarehouse(player)) {
				// 无法存入仓库。 / Cannot store this in warehouse.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300418));
				return true;
			}
			break;
		case ACCOUNT_WAREHOUSE:
			if (!item.isStorableinAccWarehouse(player)) {
				// 无法存入账号仓库。 / Cannot store this in account warehouse.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400356));
				return true;
			}
			break;
		case LEGION_WAREHOUSE:
			if (!item.isStorableinLegWarehouse(player) || !LegionConfig.LEGION_WAREHOUSE) {
				// 无法存入军团仓库。 / Cannot store this in legion warehouse.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400355));
				return true;
			} else if (!player.isLegionMember() || !GameCoreGameplayServices.legionService().getLegionMember(player.getObjectId())
					.hasRights(LegionPermissionsMask.WH_DEPOSIT)) {
				// 无权使用军团仓库。 / No authority to use legion warehouse.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1300322));
				return true;
			}
			break;
		}
		return false;
	}

}
