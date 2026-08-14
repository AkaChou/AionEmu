package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员小队召唤命令：将指定玩家所在小队成员传送至管理员身边。
 * Admin group-to-me command: teleport members of a named player's group to the admin.
 *
 * @author Source
 */
public class GroupToMe extends AdminCommand {

	public GroupToMe() {
		super("grouptome");
	}

	/**
	 * 将目标玩家小队（除管理员自身）传送到管理员坐标。
	 * Teleport the target player's group (except the admin) to the admin's position.
	 *
	 * @param admin 执行命令的管理员 / Admin executing the command
	 * @param params 目标玩家名 / Target player name
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			onFail(admin, null);
			return;
		}

		Player groupToMove = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (groupToMove == null) {
			PacketSendUtility.sendMessage(admin, "The player is not online.");
			return;
		}

		if (!groupToMove.isInGroup2()) {
			PacketSendUtility.sendMessage(admin, groupToMove.getName() + " is not in group.");
			return;
		}

		for (Player target : groupToMove.getPlayerGroup2().getMembers())
			if (target != admin) {
				TeleportService2.teleportTo(target, admin.getWorldId(), admin.getInstanceId(), admin.getX(), admin.getY(),
					admin.getZ(), admin.getHeading());
				PacketSendUtility.sendMessage(target, "You have been summoned by " + admin.getName() + ".");
				PacketSendUtility.sendMessage(admin, "You summon " + target.getName() + ".");
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
		PacketSendUtility.sendMessage(player, "syntax //grouptome <player>");
	}
}
