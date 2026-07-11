package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员添加技能命令：为目标玩家学习指定技能等级。
 * Admin add-skill command: teaches a skill at a given level to the target player.
 *
 * @author Phantom
 */
public class AddSkill extends AdminCommand {

	/**
	 * 注册 {@code //addskill} 命令。
	 * Registers the {@code //addskill} command.
	 */
	public AddSkill() {
		super("addskill");
	}

	/**
	 * 执行添加技能：解析技能 ID/等级并授予目标玩家。
	 * Executes add-skill: parses skill id/level and grants it to the target player.
	 *
	 * admin
	 * @param params 参数：技能 ID、技能等级 / skill id, skill level
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length != 2) {
			PacketSendUtility.sendMessage(player, "syntax //addskill <skillId> <skillLevel>");
			return;
		}

		VisibleObject target = player.getTarget();

		int skillId = 0;
		int skillLevel = 0;

		try {
			skillId = Integer.parseInt(params[0]);
			skillLevel = Integer.parseInt(params[1]);
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(player, "Parameters need to be integers.");
			return;
		}

		if (target instanceof Player) {
			Player targetpl = (Player) target;
			targetpl.getSkillList().addSkill(targetpl, skillId, skillLevel);
			PacketSendUtility.sendMessage(player, "You have success add skill");
			PacketSendUtility.sendMessage(targetpl, "You have acquire a new skill");
		}
	}

	/**
	 * 参数错误时输出 {@code //addskill} 用法。
	 * Prints {@code //addskill} usage on invalid arguments.
	 *
	 * admin
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //addskill <skillId> <skillLevel>");
	}
}