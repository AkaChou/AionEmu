package com.aionemu.gameserver.commands.admin;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员角色封禁命令：按角色名封禁指定天数并记录原因。
 * Admin character-ban command: bans a character by name for N days with a reason.
 *
 * @author nrg
 */
public class BanChar extends AdminCommand {

	/**
	 * 注册 {@code //banchar} 命令。
	 * Registers the {@code //banchar} command.
	 */
	public BanChar() {
		super("banchar");
	}

	/**
	 * 执行角色封禁：解析角色、天数与原因后调用惩罚服务。
	 * Executes character ban: resolves char, days and reason, then calls punishment service.
	 *
	 * admin
	 * @param params 参数：玩家名、天数、原因 / player name, days, reason
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 3) {
			sendInfo(admin, true);
			return;
		}
		
		int playerId = 0;
		String playerName = Util.convertName(params[0]);

		// 首先尝试在世界中查找玩家 / First, try to find player in the World
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerName);
		if (player != null)
			playerId = player.getObjectId();

		// 其次，尝试从数据库获取离线玩家 ID / Second, try to get player Id from offline player from database
		if (playerId == 0)
			playerId = DAOManager.getDAO(PlayerDAO.class).getPlayerIdByName(playerName);

		// 第三，失败 / Third, fail
		if (playerId == 0) {
			PacketSendUtility.sendMessage(admin, "Player " + playerName + " was not found!");
			sendInfo(admin, true);
			return;
		}

		int dayCount = -1;
		try {
			dayCount = Integer.parseInt(params[1]);
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "Second parameter is not an int");
			sendInfo(admin, true);
			return;
		}
		
		if(dayCount < 0) {
			PacketSendUtility.sendMessage(admin, "Second parameter has to be a positive daycount or 0 for infinity");
			sendInfo(admin, true);;
			return;
		}

		String reason = Util.convertName(params[2]);
		for(int itr = 3; itr < params.length; itr++)
			reason += " "+params[itr];

		PacketSendUtility.sendMessage(admin, "Char " + playerName + " is now banned for the next "+dayCount+" days!");
		
		PunishmentService.banChar(playerId, dayCount, reason);
	}

	/**
	 * 参数错误时输出 {@code //banchar} 用法。
	 * Prints {@code //banchar} usage on invalid arguments.
	 *
	 * admin
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		sendInfo(player, false);
	}
	
	/**
	 * 向管理员发送 {@code //banchar} 语法说明。
	 * Sends {@code //banchar} syntax help to the admin.
	 *
	 * admin
	 * @param withNote 是否附带天数说明 / whether to include the day-count note
	 */
	private void sendInfo(Player player, boolean withNote) {
		PacketSendUtility.sendMessage(player, "Syntax: //banChar <playername> <days>/0 (for permanent) <reason>");
		if(withNote)
		  PacketSendUtility.sendMessage(player, "Note: the current day is defined as a whole day even if it has just a few hours left!");
	}
}