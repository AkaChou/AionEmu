package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.NoSuchElementException;

/**
 * 将在线玩家从监狱释放的管理员命令。
 * Admin command to release an online player from prison.
 * Command: //rprison &lt;player&gt;
 *
 * @author lord_rex Command: //rprison <player> This command is removing player from prison.
 */
public class RPrison extends AdminCommand {

	/**
	 * 以别名 {@code rprison} 构造命令。
	 * Construct the command with alias {@code rprison}.
	 */
	public RPrison() {
		super("rprison");
	}

	/**
	 * 解除指定在线玩家的监狱状态。
	 * Clear the prison state of the named online player.
	 *
	 * 执行 GM / Admin player
	 * Character name
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0 || params.length > 2) {
			PacketSendUtility.sendMessage(admin, "syntax //rprison <player>");
			return;
		}

		try {
			Player playerFromPrison = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));

			if (playerFromPrison != null) {
				PunishmentService.setIsInPrison(playerFromPrison, false, 0, "");
				PacketSendUtility.sendMessage(admin, "Player " + playerFromPrison.getName() + " removed from prison.");
			}
		}
		catch (NoSuchElementException nsee) {
			PacketSendUtility.sendMessage(admin, "Usage: //rprison <player>");
		}
		catch (Exception e) {
			PacketSendUtility.sendMessage(admin, "Usage: //rprison <player>");
		}
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //rprison <player>");
	}
}
