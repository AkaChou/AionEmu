package com.aionemu.gameserver.commands.player;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_PLAYER_APPEARANCE;
import com.aionemu.gameserver.services.item.ItemRemodelService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：消耗材料将已装备同类部位改模为指定外观。
 * Player command: spends material to remodel an equipped same-slot item to a given look.
 *
 * @author Kashim
 */
public class cmd_remodel extends PlayerCommand {

	/**
	 * 注册命令别名 {@code remodel}。
	 * Registers the command alias {@code remodel}.
	 */
	public cmd_remodel() {
		super("remodel");
	}

	/**
	 * 扣除改模材料并对匹配装备执行系统改模。
	 * Consumes remodel material and applies system remodel on a matching equipped item.
	 *
	 * @param admin 执行命令的玩家 / invoking player
	 * @param params 目标外观物品 ID / target appearance item id
	 */
	public void executeCommand(Player admin, String[] params) {

		if (params.length < 1) {
			PacketSendUtility.sendMessage(admin, "Syntax: .remodel <itemid>\n");
			return;
		}

		if (params.length == 1) { // Use target
			int itemId = Integer.parseInt(params[0]);
			if (admin.getInventory().decreaseByItemId(186000202, 1)) {
				if (remodelItem(admin, itemId)) {
					PacketSendUtility.sendMessage(admin, "Successfully remodelled an item of the player!");
					PacketSendUtility.broadcastPacket(admin, new SM_UPDATE_PLAYER_APPEARANCE(admin.getObjectId(), admin.getEquipment().getEquippedItemsWithoutStigma()), true);
				}
				else {
					PacketSendUtility.sendMessage(admin, "Was not able to remodel an item of the player!");
				}
			}
			else {
				PacketSendUtility.sendMessage(admin, "You do not meet the requirements !");
			}
		}
	}

	private boolean remodelItem(Player player, int itemId) {
		ItemTemplate template = DataManager.ITEM_DATA.getItemTemplate(itemId);
		if (template == null) {
			return false;
		}

		Equipment equip = player.getEquipment();
		if (equip == null) {
			return false;
		}

		for (Item item : equip.getEquippedItemsWithoutStigma()) {
			if (item.getItemTemplate().isWeapon()) {
				if (item.getItemTemplate().getWeaponType() == template.getWeaponType()) {
					ItemRemodelService.systemRemodelItem(player, item, template);
					PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, item));
					DAOManager.getDAO(InventoryDAO.class).store(item, player);
					return true;
				}
			}
			else if (item.getItemTemplate().isArmor()) {
				if (item.getItemTemplate().getItemSlot() == template.getItemSlot()) {
					ItemRemodelService.systemRemodelItem(player, item, template);
					PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, item));
					DAOManager.getDAO(InventoryDAO.class).store(item, player);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 将可变参数转发到 {@link #executeCommand(Player, String[])}。
	 * Forwards varargs to {@link #executeCommand(Player, String[])}.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * command parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		executeCommand(player, params);
	}
}
