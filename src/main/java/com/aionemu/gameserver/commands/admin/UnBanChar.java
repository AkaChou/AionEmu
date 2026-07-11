package com.aionemu.gameserver.commands.admin;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除角色封禁的管理员命令。
 * Admin command to lift a character ban.
 *
 * @author nrg
 */
public class UnBanChar extends AdminCommand {

	/**
	 * 构造 unbanchar 命令。
	 * Creates the unbanchar command.
	 */
	public UnBanChar() {
		super("unbanchar");
	}

	/**
	 * 按角色名解除角色封禁。
	 * Unbans a character by name.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params &lt;player&gt;。 / &lt;player&gt;
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "Syntax: //unbanchar <player>");
			return;
		}

		// 被封禁玩家须离线，故从数据库取其账号 ID / Banned player must be offline
		String name = Util.convertName(params[0]);
		int playerId = DAOManager.getDAO(PlayerDAO.class).getPlayerIdByName(name);
		if (playerId == 0) {
			PacketSendUtility.sendMessage(admin, "Player " + name + " was not found!");
			PacketSendUtility.sendMessage(admin, "Syntax: //unbanchar <player>");
			return;
		}

		PacketSendUtility.sendMessage(admin, "Character " + name + " is not longer banned!");

    PunishmentService.unbanChar(playerId);
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //unban <player> [account|ip|full]");
	}
}
