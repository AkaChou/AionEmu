package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CubeExpandService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import java.util.Collection;

/**
 * @author Waii
 * new adaptation made by Dezalmado
 */
public final class CmdSetInventoryGrowth extends AbstractGMHandler {

    public CmdSetInventoryGrowth(Player admin, String params) {
        super(admin, params);
        if (this.admin == null) {
            System.err.println("CmdSetInventoryGrowth: Admin player is null. Cannot execute command.");
            return;
        }

        if (this.admin.getAccessLevel() < AdminConfig.GM_LEVEL) {
            PacketSendUtility.sendMessage(this.admin, "You do not have sufficient access level to use this command.");
            return;
        }

        run();
    }

    public void run() {
        Player playerToExpand = null;
        String[] commandArgs = this.params.split(" ");


        if (this.admin.getTarget() instanceof Player) {
            playerToExpand = (Player) this.admin.getTarget();
        }


        if (playerToExpand == null && commandArgs.length > 0 && !commandArgs[0].isEmpty()) {
            String targetPlayerName = commandArgs[0];
            Collection<Player> allPlayers = World.getInstance().getAllPlayers();
            for (Player p : allPlayers) {
                if (p.getName().equalsIgnoreCase(targetPlayerName)) {
                    playerToExpand = p;
                    break;
                }
            }
        }

        if (playerToExpand == null) {
            PacketSendUtility.sendMessage(this.admin, "Error: Player not found for expansion or incorrect usage. Use: //setinventorygrowth [player_name] or select a target.");
            return;
        }

        if (CubeExpandService.canExpand(playerToExpand)) {
            CubeExpandService.expand(playerToExpand, true);
            PacketSendUtility.sendMessage(this.admin, "9 cube slots successfully added to player " + playerToExpand.getName() + "!");
            if (!playerToExpand.equals(this.admin)) {
                PacketSendUtility.sendMessage(playerToExpand, "Admin " + this.admin.getName() + " granted you a cube expansion!");
            }
        } else {
            PacketSendUtility.sendMessage(this.admin, "Cube expansion cannot be added to " + playerToExpand.getName() + "! Reason: player cube already fully expanded.");
        }
    }
}