package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 将自身或目标玩家变形为指定 NPC 外观的管理员命令。
 * Admin command to morph self or a targeted player into an NPC appearance.
 */
public class Morph extends AdminCommand
{
	/**
	 * 以别名 {@code morph} 构造命令。
	 * Construct the command with alias {@code morph}.
	 */
	public Morph() {
		super("morph");
	}

	/**
	 * 将目标（默认自身）变形为给定 NPC Id，或使用 {@code cancel} 取消变形。
	 * Morph the target (self by default) into the given NPC id, or {@code cancel} the morph.
	 *
	 * 执行 GM / Admin player
	 * NPC id or cancel
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length != 1) {
			PacketSendUtility.sendMessage(admin, "syntax //morph <NPC Id | cancel> ");
			return;
		}
		Player target = admin;
		int param = 0;
		if (admin.getTarget() instanceof Player) {
			target = (Player) admin.getTarget();
		} if (!("cancel").startsWith(params[0].toLowerCase())) {
			try {
				param = Integer.parseInt(params[0]);
			} catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "Parameter must be an integer, or cancel.");
				return;
			}
		} if ((param != 0 && param < 200000) || param > 999999) {
			PacketSendUtility.sendMessage(admin, "Something wrong with the NPC Id!");
			return;
		}
		target.getTransformModel().setModelId(param);
		PacketSendUtility.broadcastPacketAndReceive(target, new SM_TRANSFORM(target, true));
		if (param == 0) {
			if (target.equals(admin)) {
				PacketSendUtility.sendMessage(target, "Morph successfully cancelled.");
			} else {
				PacketSendUtility.sendMessage(target, "Your morph has been cancelled by " + admin.getName() + ".");
				PacketSendUtility.sendMessage(admin, "You have cancelled " + target.getName() + "'s morph.");
			}
		} else {
			if (target.equals(admin)) {
				PacketSendUtility.sendMessage(target, "Successfully morphed to npcId " + param + ".");
			} else {
				PacketSendUtility.sendMessage(target, admin.getName() + " morphs you into an NPC form.");
				PacketSendUtility.sendMessage(admin, "You morph " + target.getName() + " to npcId " + param + ".");
			}
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
		PacketSendUtility.sendMessage(player, "syntax //morph <NPC Id | cancel> ");
	}
}
