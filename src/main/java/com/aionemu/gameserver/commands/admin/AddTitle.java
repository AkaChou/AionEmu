package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员添加称号命令：为目标玩家解锁称号。
 * Admin add-title command: unlocks a title for the target player.
 */
public class AddTitle extends AdminCommand
{
	/**
	 * 注册 {@code //addtitle} 命令。
	 * Registers the {@code //addtitle} command.
	 */
	public AddTitle() {
		super("addtitle");
	}
	
	/**
	 * 执行添加称号：按种族偏移解析称号 ID 并授予目标。
	 * Executes add-title: resolves race-offset title id and grants it to the target.
	 *
	 * @param params 参数：称号 ID、玩家名（可选） / title id, optional player name
	 */
	@Override
	public void execute(Player player, String... params) {
		if ((params.length < 1) || (params.length > 2)) {
			onFail(player, null);
			return;
		}
		int titleId = Integer.parseInt(params[0]);
		if ((titleId > 377) || (titleId < 1)) {
			PacketSendUtility.sendMessage(player, "title id " + titleId + " is invalid (must be between 1 and 327)");
			return;
		}
		Player target = null;
		if (params.length == 2) {
			target = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[1]));
			if (target == null) {
				PacketSendUtility.sendMessage(player, "player " + params[1] + " was not found");
				return;
			}
		} else {
			VisibleObject creature = player.getTarget();
			if (player.getTarget() instanceof Player) {
				target = (Player) creature;
			} if (target == null) {
				target = player;
			}
		} if (titleId < 378) {
			titleId = target.getRace().getRaceId() * 377 + titleId;
		} if (!target.getTitleList().addTitle(titleId, false, 0)) {
			PacketSendUtility.sendMessage(player, "you can't add title #" + titleId + " to " + (target.equals(player) ? "yourself" : target.getName()));
		} else {
			if (target.equals(player)) {
				PacketSendUtility.sendMessage(player, "you added to yourself title #" + titleId);
			} else {
				PacketSendUtility.sendMessage(player, "you added to " + target.getName() + " title #" + titleId);
				PacketSendUtility.sendMessage(target, player.getName() + " gave you title #" + titleId);
			}
		}
	}
	
	/**
	 * 参数错误时输出 {@code //addtitle} 用法。
	 * Prints {@code //addtitle} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //addtitle title_id [playerName]");
	}
}