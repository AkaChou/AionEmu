package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员地理高度查询命令：对比 GeoService 计算 Z 与玩家当前 Z。
 * Admin geo height query command: compare GeoService-computed Z with the player's current Z.
 *
 * @author MrPoke
 */
public class Geo extends AdminCommand{

	public Geo() {
		super("geo");
	}

	/**
	 * 查询当前坐标的地理高度（子命令 z）。
	 * Query geo height at the current position (subcommand z).
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params 子命令参数 / Subcommand parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		if ("z".startsWith(params[0])){
			PacketSendUtility.sendMessage(player, "GeoZ: "+GameWorldServices.geoService().getZ(player)+ " current Z: "+player.getZ());
		}
	}
}
