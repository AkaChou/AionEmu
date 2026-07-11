package com.aionemu.gameserver;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.services.EnchantService;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：将已装备的可强化物品统一设为指定强化等级（{@code .enchant}）。
 * Player command that sets equipped upgradeable items to a given enchant level ({@code .enchant}).
 *
 * @author Tago, Wakizashi, Ney, Maestros, Eloann
 */
public class cmd_enchant extends PlayerCommand {

	/**
	 * 注册命令名为 {@code enchant}。
	 * Registers the command name {@code enchant}.
	 */
	public cmd_enchant() {
		super("enchant");
	}

	/**
	 * 解析强化等级并应用到玩家已装备的可强化物品。
	 * Parses enchant level and applies it to the player's equipped upgradeable items.
	 *
	 * @param player 玩家 / player
	 * @param params 强化等级 / enchant level
	 */
    @Override
    public void execute(Player player, String... params) {
        int enchant = 0;

        try {
            enchant = params[0] == null ? enchant : Integer.parseInt(params[0]);
        } catch (Exception ex) {
            onFail(player, "Fail");
            return;
        }
        int maxEnchant = EnchantService.getMaxEquipmentEnchantLevel();
        if(enchant <= maxEnchant){
            enchant(player, enchant);
        } else{
            PacketSendUtility.sendMessage(player, "You cannot enchant higher than +" + maxEnchant + "!");
        }
    }

	/**
	 * 将所有可强化的已装备物品设为指定强化等级。
	 * Sets all upgradeable equipped items to the given enchant level.
	 *
	 * @param player 玩家 / player
	 * @param enchant 目标强化等级 / target enchant level
	 */
	private void enchant(Player player, int enchant) {
		for (Item targetItem : player.getEquipment().getEquippedItemsWithoutStigma()) {
			if (isUpgradeble(targetItem)) {
				int enchantLevel = EnchantService.capEquipmentEnchantLevel(enchant);

				targetItem.setEnchantLevel(enchantLevel);

				if (targetItem.isEquipped()) {
					player.getGameStats().updateStatsVisually();
				}
				ItemPacketService.updateItemAfterInfoChange(player, targetItem);
			}
		}
		PacketSendUtility.sendMessage(player, "All your items have been enchanted to: " + enchant);
	}

	/**
	 * 判断物品是否可强化（武器/护甲槽位，非禁强化/非 stigma，未达上限）。
	 * Whether the item is upgradeable (weapon/armor slots, not no-enchant/stigma, below max level).
	 *
	 * item
	 *
	 * @param item @return 可强化返回 true / {@code true} if upgradeable
	 */
	public static boolean isUpgradeble(Item item) {
		if (item.getItemTemplate().isNoEnchant()) {
			return false;
		}
		if (item.getItemTemplate().isWeapon()) {
			return true;
		}
		if (item.getItemTemplate().getCategory() == ItemCategory.STIGMA) {
			return false;
		}
		if (item.getEnchantLevel() >= EnchantService.getMaxEquipmentEnchantLevel()) {
			return false;
		}
		if (item.getItemTemplate().isArmor()) {
			int at = item.getItemTemplate().getItemSlot();
			if (at == 1 || /* Main Hand */at == 2 || /* Sub Hand */at == 8 || /* Jacket */at == 16 || /* Gloves */at == 32 || /* Boots */at == 2048 || /* Shoulder */at == 4096 || /* Pants */at == 131072
				|| /*
					 * Main Off Hand
					 */at == 262144) /*
									 * Sub Off Hand
									 */ {
				return true;
			}
		}
		return false;
	}

	/**
	 * 发送命令语法帮助。
	 * Sends command syntax help.
	 *
	 * @param player 玩家 / player
	 * @param message 可选消息 / optional message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax .enchant : \n" + "  Syntax .enchant <value>.\n" + "Info: This command all your enchanted items on <value>!" + " For example, would enchant all your items to 15 (eq 15.)");
	}
}
