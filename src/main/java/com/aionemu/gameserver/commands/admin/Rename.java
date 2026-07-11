package com.aionemu.gameserver.commands.admin;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dao.OldNamesDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_UPDATE_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_MOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.NameRestrictionService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.Iterator;

/**
 * 角色改名指令；校验名称后重命名目标或指定玩家，并同步好友与军团。
 * Admin command that renames a targeted or named player after validation, syncing friends and legion.
 *
 * @author xTz
 */
public class Rename extends AdminCommand {

	public Rename() {
		super("rename");
	}

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1 || params.length > 2) {
			PacketSendUtility.sendMessage(admin, "No parameters detected.\n" + "Please use //rename <Player name> <rename>\n"
				+ "or use //rename [target] <rename>");
			return;
		}

		Player player = null;
		String recipient = null;
		String rename = null;

		if (params.length == 2) {
			recipient = Util.convertName(params[0]);
			rename = Util.convertName(params[1]);

			if (!DAOManager.getDAO(PlayerDAO.class).isNameUsed(recipient)) {
				PacketSendUtility.sendMessage(admin, "Could not find a Player by that name.");
				return;
			}
			PlayerCommonData recipientCommonData = DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonDataByName(recipient);
			player = recipientCommonData.getPlayer();

			if (!check(admin, rename))
				return;

			if (!CustomConfig.OLD_NAMES_COMMAND_DISABLED)
				DAOManager.getDAO(OldNamesDAO.class).insertNames(player.getObjectId(), player.getName(), rename);
			recipientCommonData.setName(rename);
			DAOManager.getDAO(PlayerDAO.class).storePlayerName(recipientCommonData);
			if (recipientCommonData.isOnline()) {
				PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
				PacketSendUtility.sendPacket(player, new SM_MOTION(player.getObjectId(), player.getMotions().getActiveMotions()));
				sendPacket(admin, player, rename, recipient);
			}
			else
				PacketSendUtility.sendMessage(admin, "Player " + recipient + " has been renamed to " + rename);
		}
		if (params.length == 1) {
			rename = Util.convertName(params[0]);

			VisibleObject target = admin.getTarget();
			if (target == null) {
				PacketSendUtility.sendMessage(admin, "You should select a target first!");
				return;
			}

			if (target instanceof Player) {
				player = (Player) target;
				if (!check(admin, rename))
					return;

				if (!CustomConfig.OLD_NAMES_COMMAND_DISABLED)
					DAOManager.getDAO(OldNamesDAO.class).insertNames(player.getObjectId(), player.getName(), rename);
				player.getCommonData().setName(rename);
				PacketSendUtility.sendPacket(player, new SM_PLAYER_INFO(player, false));
				DAOManager.getDAO(PlayerDAO.class).storePlayerName(player.getCommonData());
			}
			else
				PacketSendUtility.sendMessage(admin, "The command can be applied only on the player.");

			recipient = target.getName();
			sendPacket(admin, player, rename, recipient);
		}
	}

	private static boolean check(Player admin, String rename) {
		if (!NameRestrictionService.isValidName(rename)) {
			PacketSendUtility.sendPacket(admin, new SM_SYSTEM_MESSAGE(1400151));
			return false;
		}
		if (!PlayerService.isFreeName(rename)) {
			PacketSendUtility.sendPacket(admin, new SM_SYSTEM_MESSAGE(1400155));
			return false;
		}
		if (!CustomConfig.OLD_NAMES_COMMAND_DISABLED && PlayerService.isOldName(rename)) {
			PacketSendUtility.sendPacket(admin, new SM_SYSTEM_MESSAGE(1400155));
			return false;
		}
		return true;
	}

	/**
	 * 向好友、军团与双方发送改名后的信息同步包。
	 * Broadcasts post-rename info packets to friends, legion and both players.
	 *
	 * @param admin 执行改名的管理员 / admin who performed the rename
	 * @param player 被改名的玩家 / renamed player
	 * new name
	 * original name
	 */
	public void sendPacket(Player admin, Player player, String rename, String recipient) {
		Iterator<Friend> knownFriends = player.getFriendList().iterator();

		while (knownFriends.hasNext()) {
			Friend nextObject = knownFriends.next();
			if (nextObject.getPlayer() != null && nextObject.getPlayer().isOnline()) {
				PacketSendUtility.sendPacket(nextObject.getPlayer(), new SM_PLAYER_INFO(player, false));
			}
		}

		if (player.isLegionMember()) {
			PacketSendUtility.broadcastPacketToLegion(player.getLegion(), new SM_LEGION_UPDATE_MEMBER(player, 0, ""));
		}
		PacketSendUtility.sendMessage(player, "You have been renamed to " + rename);
		PacketSendUtility.sendMessage(admin, "Player " + recipient + " has been renamed to " + rename);
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "No parameters detected.\n" + "Please use //rename <Player name> <rename>\n" + "or use //rename [target] <rename>");
	}
}