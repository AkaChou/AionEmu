package com.aionemu.gameserver.commands.admin;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerPasskeyDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 强制重置玩家账号二级密码（Passkey）的管理员命令。
 * Admin command to force-reset a player's account passkey.
 *
 * @author cura
 */
public class PasskeyReset extends AdminCommand {

	/**
	 * 以别名 {@code passkeyreset} 构造命令。
	 * Construct the command with alias {@code passkeyreset}.
	 */
	public PasskeyReset() {
		super("passkeyreset");
	}

	/**
	 * 按角色名解析账号并写入新的 6–8 位数字二级密码，随后通知登录服。
	 * Resolve the account by character name, write a new 6–8 digit passkey, and notify the login server.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params 角色名与新 passkey / Character name and new passkey
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "syntax: //passkeyreset <player> <passkey>");
			return;
		}

		String name = Util.convertName(params[0]);
		int accountId = DAOManager.getDAO(PlayerDAO.class).getAccountIdByName(name);
		if (accountId == 0) {
			PacketSendUtility.sendMessage(player, "player " + name + " can't find!");
			PacketSendUtility.sendMessage(player, "syntax: //passkeyreset <player> <passkey>");
			return;
		}

		try {
			Integer.parseInt(params[1]);
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(player, "parameters should be number!");
			return;
		}

		String newPasskey = params[1];
		if (!(newPasskey.length() > 5 && newPasskey.length() < 9)) {
			PacketSendUtility.sendMessage(player, "passkey is 6~8 digits!");
			return;
		}

		DAOManager.getDAO(PlayerPasskeyDAO.class).updateForcePlayerPasskey(accountId, newPasskey);
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendBanPacket((byte) 2, accountId, "", -1, player.getObjectId());
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax: //passkeyreset <player> <passkey>");
	}
}
