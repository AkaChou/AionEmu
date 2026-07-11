package com.aionemu.gameserver.services.events;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.configs.main.*;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.events.*;
import com.aionemu.gameserver.utils.*;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * FFA 玩家命令，提供进入/离开自由混战的指令入口。
 * FFA player command providing enter/leave free-for-all entry points.
 */


public class cmd_ffa extends PlayerCommand
{
    public cmd_ffa() {
        super("ffa");
    }

    @Override
    /**
     * 执行命令。
     * Executes the command.
     *
     * 玩家 / player
     * params
     */
    public void execute(Player player, String... params) {
        if (!FFAConfig.FFA_ENABLED) {
            PacketSendUtility.sendSys3Message(player, "\uE00B", "<FFA> is disabled!!!");
            return;
        } if (player.getLevel() < 10) {
            PacketSendUtility.sendSys3Message(player, "\uE00B", "<FFA> You must reached lvl 10!");
            return;
        } if (player.isInInstance() && !GameFeatureServices.ffaService().isInArena(player) && !player.isFFA()) {
            PacketSendUtility.sendSys3Message(player, "\uE00B", "<FFA> You can't use <FFA> mod in instance!!!");
            return;
        } if (player.getBattleground() != null || GameFeatureServices.ladderService().isInQueue(player) || player.isSpectating()||player.getLifeStats().isAlreadyDead()) {
            PacketSendUtility.sendSys3Message(player, "\uE00B", "<FFA> You cannot enter <FFA> while in a battleground, in the queue, while spectating or being dead !!!");
            return;
        } if (GameFeatureServices.ffaService().isInArena(player)) {
            PacketSendUtility.sendSys3Message(player, "\uE00B", "<FFA> You will be leaving <FFA> in 10 seconds!");
            GameFeatureServices.ffaService().leaveArena(player);
        } else {
            if (player.getController().isInCombat()) {
                PacketSendUtility.sendSys3Message(player, "\uE00B", "<FFA> You cannot enter <FFA> while in combat.");
                return;
            }
            PacketSendUtility.sendSys3Message(player, "\uE00B", "<FFA> You will be entering <FFA> in 10 seconds. To leave <FFA> write .ffa!!!");
            GameFeatureServices.ffaService().enterArena(player, false);
        }
    }
}