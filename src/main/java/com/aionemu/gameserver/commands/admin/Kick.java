package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUIT_RESPONSE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员踢人命令：断开指定玩家或全部非 GM 玩家连接。
 * Admin kick command: disconnect a named player or all non-GM players.
 *
 * @author Elusive
 */
public class Kick extends AdminCommand {

	public Kick() {
		super("kick");
	}

	/**
	 * 踢出指定角色，或使用 All 踢出所有非 GM。
	 * Kick the named character, or All for every non-GM.
	 *
	 * @param admin 执行命令的管理员 / Admin executing the command
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1) {
			PacketSendUtility.sendMessage(admin, "syntax //kick <character_name> | <All>");
			return;
		}

		if(params[0] != null && "All".equalsIgnoreCase(params[0])){
			for (final Player player : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
				if(!player.isGM()){
					player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
					PacketSendUtility.sendMessage(admin, "Kicked player : " + player.getName());
				}
			}
		}else{
			Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
			if (player == null) {
				PacketSendUtility.sendMessage(admin, "The specified player is not online.");
				return;
			}
			player.getClientConnection().close(new SM_QUIT_RESPONSE(), false);
			PacketSendUtility.sendMessage(admin, "Kicked player : " + player.getName());
		}

	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //kick <character_name> | <All>");
	}
}
