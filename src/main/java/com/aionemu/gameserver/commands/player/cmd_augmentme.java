package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemChargeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：对全身已装备物品执行充能/调校并扣除基纳。
 * Player command: charges/augments all equipped items and deducts Kinah.
 *
 * @author Kill3r
 */
public class cmd_augmentme extends PlayerCommand {

    /**
     * 注册命令别名 {@code augmentme}。
     * Registers the command alias {@code augmentme}.
     */
    public cmd_augmentme() {
        super("augmentme");
    }

    /**
     * 对已装备物品执行二级充能，并扣除 500000 基纳。
     * Charges equipped items to level 2 and deducts 500000 Kinah.
     *
     * @param player 执行命令的玩家 / invoking player
     * @param params 未使用的参数 / unused parameters
     */
    public void execute(Player player, String...params){

        ItemChargeService.chargeItems(player,player.getEquipment().getEquippedItems(), 2);
        PacketSendUtility.sendMessage(player, "You've Successfuly Augmented you're Gear!");
        player.getInventory().decreaseKinah(500000);
    }
}
