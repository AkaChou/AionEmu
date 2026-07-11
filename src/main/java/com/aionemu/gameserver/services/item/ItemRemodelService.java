package com.aionemu.gameserver.services.item;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_PLAYER_APPEARANCE;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 物品幻化服务，处理装备外观改造与预览。
 * Item remodel service handling equipment appearance remodel and preview.
 */
public class ItemRemodelService {
	/**
	 * 幻化物品。
	 * Remodels an item appearance.
	 *
	 * @param player 玩家 / player
	 * @param keepItemObjId 保留物品对象 ID / keepItemObjId
	 * @param extractItemObjId 提取物品对象 ID / extractItemObjId
	 */
	public static void remodelItem(Player player, int keepItemObjId, int extractItemObjId) {
		Storage inventory = player.getInventory();
		Item keepItem = inventory.getItemByObjId(keepItemObjId);
		Item extractItem = inventory.getItemByObjId(extractItemObjId);
		int remodelKinah = 14125;
		if (keepItem == null && extractItem == null) {
			return;
		}
		if (player.getLevel() < 10) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CHANGE_ITEM_SKIN_PC_LEVEL_LIMIT);
			return;
		}
		if (player.getInventory().getKinah() < remodelKinah) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
					.STR_CHANGE_ITEM_SKIN_NOT_ENOUGH_GOLD(new DescriptionId(keepItem.getItemTemplate().getNameId())));
			return;
		}
		if (extractItem == null) {
			if (keepItem.getItemTemplate() == keepItem.getItemSkinTemplate()) {
				PacketSendUtility.sendMessage(player, "That item does not have a remodeled skin to remove.");
				return;
			}
			if (player.getInventory().getKinah() >= remodelKinah) {
				player.getInventory().decreaseKinah(remodelKinah);
			}
			keepItem.setItemSkinTemplate(keepItem.getItemTemplate());
			if (!keepItem.getItemTemplate().isItemDyePermitted()) {
				keepItem.setItemColor(0);
			}
			ItemPacketService.updateItemAfterInfoChange(player, keepItem);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
					.STR_CHANGE_ITEM_SKIN_SUCCEED(new DescriptionId(keepItem.getItemTemplate().getNameId())));
			return;
		}
		if ((keepItem.getItemTemplate().getWeaponType() != extractItem.getItemSkinTemplate().getWeaponType())) {
			PacketSendUtility.sendPacket(player,
					SM_SYSTEM_MESSAGE.STR_CHANGE_ITEM_SKIN_NOT_COMPATIBLE(
							new DescriptionId(keepItem.getItemTemplate().getNameId()),
							new DescriptionId(extractItem.getItemSkinTemplate().getNameId())));
			return;
		}
		if (!keepItem.isRemodelable(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_SYSTEM_MESSAGE(1300478, new DescriptionId(keepItem.getItemTemplate().getNameId())));
			return;
		}
		if (!extractItem.isRemodelable(player)) {
			PacketSendUtility.sendPacket(player,
					new SM_SYSTEM_MESSAGE(1300482, new DescriptionId(keepItem.getItemTemplate().getNameId())));
			return;
		}
		if (player.getInventory().getKinah() >= remodelKinah) {
			player.getInventory().decreaseKinah(remodelKinah);
		}
		player.getInventory().decreaseItemCount(extractItem, 1);
		keepItem.setItemSkinTemplate(extractItem.getItemSkinTemplate());
		keepItem.setItemColor(extractItem.getItemColor());
		keepItem.setItemSkinSkill(extractItem.getItemSkinSkill());
		ItemPacketService.updateItemAfterInfoChange(player, keepItem);
		PacketSendUtility.sendPacket(player,
				new SM_SYSTEM_MESSAGE(1300483, new DescriptionId(keepItem.getItemTemplate().getNameId())));
	}

	/**
	 * 系统幻化物品。
	 * System-remodels an item.
	 *
	 * 玩家 / player
	 * keepItem
	 * template
	 */
	public static void systemRemodelItem(Player player, Item keepItem, ItemTemplate template) {
		if (keepItem.getItemSkinSkill() > 0) {
			SkillLearnService.removeSkill(player, keepItem.getItemSkinSkill());
		}
		keepItem.setItemSkinTemplate(template);
		keepItem.setItemSkinSkill(template.getSkinSkill());
		ItemPacketService.updateItemAfterInfoChange(player, keepItem);
		PacketSendUtility.sendPacket(player,
				new SM_UPDATE_PLAYER_APPEARANCE(player.getObjectId(), player.getEquipment().getEquippedForApparence()));
		PacketSendUtility.sendPacket(player,
				new SM_SYSTEM_MESSAGE(1300483, new DescriptionId(keepItem.getItemTemplate().getNameId())));
		if (keepItem.getItemSkinSkill() > 0) {
			player.getSkillList().addSkill(player, keepItem.getItemSkinSkill(), 1);
		}
	}

	/**
	 * 命令预览幻化。
	 * Command preview remodel.
	 *
	 * 玩家 / player
	 * itemId
	 * duration
	 * result
	 */
	public static boolean commandViewRemodelItem(Player player, int itemId, int duration) {
		ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (template == null) {
			return false;
		}
		Equipment equip = player.getEquipment();
		if (equip == null) {
			return false;
		}
		for (Item item : equip.getEquippedItemsWithoutStigmaOld()) {
			if (item.getEquipmentSlot() == ItemSlot.MAIN_OFF_HAND.getSlotIdMask()
					|| item.getEquipmentSlot() == ItemSlot.SUB_OFF_HAND.getSlotIdMask()) {
				continue;
			}
			if (item.getItemTemplate().isWeapon()) {
				if (item.getItemTemplate().getWeaponType() == template.getWeaponType()
						&& item.getItemSkinTemplate().getTemplateId() != itemId) {
					viewRemodelItem(player, item, template, duration);
					return true;
				}
			} else if (item.getItemTemplate().isArmor()) {
				if (item.getItemTemplate().getItemSlot() == template.getItemSlot()
						&& item.getItemSkinTemplate().getTemplateId() != itemId) {
					viewRemodelItem(player, item, template, duration);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 预览幻化。
	 * Views remodel preview.
	 *
	 * 玩家 / player
	 * item
	 * template
	 * duration
	 */
	public static void viewRemodelItem(final Player player, final Item item, ItemTemplate template, int duration) {
		final ItemTemplate oldTemplate = item.getItemSkinTemplate();
		item.setItemSkinTemplate(template);
		PacketSendUtility.sendPacket(player,
				new SM_UPDATE_PLAYER_APPEARANCE(player.getObjectId(), player.getEquipment().getEquippedForApparence()));
		PacketSendUtility.sendPacket(player,
				new SM_SYSTEM_MESSAGE(1300483, new DescriptionId(item.getItemTemplate().getNameId())));
		PacketSendUtility.broadcastPacket(player, new SM_UPDATE_PLAYER_APPEARANCE(player.getObjectId(),
				player.getEquipment().getEquippedItemsWithoutStigma()), true);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				item.setItemSkinTemplate(oldTemplate);
			}
		}, 50);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			/**
			 * 执行任务。
			 * Runs the task.
			 */
			public void run() {
				PacketSendUtility.sendPacket(player, new SM_UPDATE_PLAYER_APPEARANCE(player.getObjectId(),
						player.getEquipment().getEquippedForApparence()));
				PacketSendUtility.broadcastPacket(player, new SM_UPDATE_PLAYER_APPEARANCE(player.getObjectId(),
						player.getEquipment().getEquippedItemsWithoutStigma()), true);
			}
		}, duration * 1000);
	}
}