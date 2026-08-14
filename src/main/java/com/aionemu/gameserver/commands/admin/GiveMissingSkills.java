package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员补齐缺失技能命令：按等级与职业为自身学习应有但未获得的技能。
 * Admin missing-skills command: learn skills the player should have by level/class but is missing.
 */
public class GiveMissingSkills extends AdminCommand
{
	public GiveMissingSkills() {
		super("givemissingskills");
	}

	/**
	 * 为执行者补齐缺失技能。
	 * Grant missing skills to the invoking player.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 */
	@Override
	public void execute(Player player, String... params) {
		SkillLearnService.addMissingSkills(player);
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	@Override
	public void onFail(Player player, String message) {
	    PacketSendUtility.sendMessage(player, "Syntax: //givemissingskills");
	}
}
