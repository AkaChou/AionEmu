package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：补全当前等级缺失的技能。
 * Player command: learns any missing skills for the current level.
 *
 * @author ATracer
 */
public class cmd_skills extends PlayerCommand {

	/**
	 * 注册命令别名 {@code skills}。
	 * Registers the command alias {@code skills}.
	 */
	public cmd_skills() {
		super("skills");
	}

	/**
	 * 调用技能学习服务补全缺失技能。
	 * Invokes the skill-learn service to add missing skills.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		SkillLearnService.addMissingSkills(player);
	}

	/**
	 * 参数错误时提示用法。
	 * Shows usage when arguments are invalid.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax : .skills");
	}
}
