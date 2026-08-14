package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 入狱指令；将在线玩家送入监狱并记录时长与原因。
 * Admin command that sends an online player to prison with a duration and reason.
 *
 * @author lord_rex
 */
public class SPrison extends AdminCommand {

	public SPrison() {
		super("sprison");
	}

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 2) {
			sendInfo(admin);
			return;
		}

		try {
			Player playerToPrison = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
			int delay = Integer.parseInt(params[1]);
			
			String reason = Util.convertName(params[2]);
			for(int itr = 3; itr < params.length; itr++)
				reason += " "+params[itr];

			if (playerToPrison != null) {
				PunishmentService.setIsInPrison(playerToPrison, true, delay, reason);
				PacketSendUtility.sendMessage(admin, "Player " + playerToPrison.getName() + " sent to prison for " + delay
					+ " because " + reason + ".");
			}
		}
		catch (Exception e) {
			sendInfo(admin);
		}
	
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 */
	@Override
	public void onFail(Player player, String message) {
		sendInfo(player);
	}
	
	private void sendInfo(Player player) {
		PacketSendUtility.sendMessage(player, "syntax //sprison <player> <delay> <reason>");
	}
}