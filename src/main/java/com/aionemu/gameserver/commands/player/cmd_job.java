package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：解锁全部主要制作技能并设为高等级。
 * Player command: unlocks all main crafting skills at high ranks.
 */
public class cmd_job extends PlayerCommand {

	/**
	 * 注册命令别名 {@code job}。
	 * Registers the command alias {@code job}.
	 */
	public cmd_job() {
		super("job");
	}

	/**
	 * 为玩家添加/提升采集与制作技能等级。
	 * Adds or upgrades gathering and crafting skill ranks for the player.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		player.getSkillList().addSkill(player, 30002, 499); // Vita
		player.getSkillList().addSkill(player, 30003, 499); // Ether
		player.getSkillList().addSkill(player, 40001, 550); // Cuisine
		player.getSkillList().addSkill(player, 40002, 550); // Armes
		player.getSkillList().addSkill(player, 40003, 550); // Armure
		player.getSkillList().addSkill(player, 40004, 550); // Couture
		player.getSkillList().addSkill(player, 40007, 550); // Alchimie
		player.getSkillList().addSkill(player, 40008, 550); // Artisanat
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
		PacketSendUtility.sendMessage(player, "Syntax: .job ");
	}
}
