package com.aionemu.gameserver.network.aion.gmhandler;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.CubeExpandService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import java.util.Collection;

/**
 * GM 指令：为指定玩家扩展背包（Cube）格子。
 * GM command handler that expands cube inventory slots for a named or targeted player.
 *
 * @author Waii
 * @modified Dezalmado
 */
@Slf4j
public final class CmdSetInventoryGrowth extends AbstractGMHandler {

	/**
	 * 创建处理器，校验权限后执行背包扩展。
	 * Creates the handler, checks access, then runs cube expansion.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 可选玩家名 / optional player name
	 */
    public CmdSetInventoryGrowth(Player admin, String params) {
        super(admin, params);
        if (this.admin == null) {
            log.warn(I18n.get("log.1a480cf6cde7", params));
            return;
        }

        if (this.admin.getAccessLevel() < AdminConfig.GM_LEVEL) {
            PacketSendUtility.sendMessage(this.admin, "You do not have sufficient access level to use this command.");
            return;
        }

        run();
    }

	/**
	 * 按选中目标或玩家名定位玩家并尝试扩展 Cube。
	 * Resolves the player by target or name and attempts a cube expansion.
	 */
    public void run() {
        Player playerToExpand = null;
        String[] commandArgs = this.params.split(" ");


        if (this.admin.getTarget() instanceof Player) {
            playerToExpand = (Player) this.admin.getTarget();
        }


        if (playerToExpand == null && commandArgs.length > 0 && !commandArgs[0].isEmpty()) {
            String targetPlayerName = commandArgs[0];
            Collection<Player> allPlayers = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers();
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
