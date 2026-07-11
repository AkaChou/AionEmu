package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 删除玩家技能的管理命令（{@code //delskill}）。
 * Admin command that removes player skills ({@code //delskill}).
 *
 * @author xTz
 */
public class DelSkill extends AdminCommand {

	/**
	 * 注册命令名为 {@code delskill}。
	 * Registers the command name {@code delskill}.
	 */
	public DelSkill() {
		super("delskill");
	}

	/**
	 * 按玩家名或当前目标删除指定技能或全部非烙印技能。
	 * Removes a skill or all non-stigma skills by player name or current target.
	 *
	 * admin
	 * @param params 玩家名与技能 ID/all，或目标下的技能 ID/all / player name and skillId/all, or skillId/all on target
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1 || params.length > 2) {
			PacketSendUtility.sendMessage(admin, "No parameters detected.\n"
				+ "Please use //delskill <Player name> <all | skillId>\n" + "or use //delskill [target] <all | skillId>");
			return;
		}

		Player player;
		PlayerSkillList playerSkillList = null;
		String recipient = null;
		recipient = Util.convertName(params[0]);
		int skillId = 0;
		if (params.length == 2) {
			player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(recipient);
			if (player == null) {
				PacketSendUtility.sendMessage(admin, "The specified player is not online.");
				return;
			}

			if ("all".startsWith(params[1]))
				playerSkillList = player.getSkillList();
			else {
				try {
					skillId = Integer.parseInt(params[1]);
				}
				catch (NumberFormatException e) {
					PacketSendUtility.sendMessage(admin, "Param 1 must be an integer or <all>.");
					return;
				}

				if (!check(admin, player, skillId))
					return;
			}
			apply(admin, player, skillId, playerSkillList);

		}
		if (params.length == 1) {
			VisibleObject target = admin.getTarget();
			if (target == null) {
				PacketSendUtility.sendMessage(admin, "You should select a target first!");
				return;
			}

			if (target instanceof Player) {
				player = (Player) target;

				if ("all".startsWith(params[0]))
					playerSkillList = player.getSkillList();
				else {
					try {
						skillId = Integer.parseInt(params[0]);
					}
					catch (NumberFormatException e) {
						PacketSendUtility.sendMessage(admin, "Param 0 must be an integer or <all>.");
						return;
					}

					if (!check(admin, player, skillId))
						return;
				}
				if (target instanceof Player)
					apply(admin, player, skillId, playerSkillList);
			}
			else
				PacketSendUtility.sendMessage(admin, "This command can only be used on a player !");
		}
	}

	/**
	 * 校验技能是否存在且非烙印技能。
	 * Validates that the skill exists and is not a stigma skill.
	 *
	 * admin
	 * target player
	 * skill id
	 *
	 * @return 可删除则为 true / true if removable
	 */
	private static boolean check(Player admin, Player player, int skillId) {
		if (skillId != 0 && !player.getSkillList().isSkillPresent(skillId)) {
			PacketSendUtility.sendMessage(admin, "Player dont have this skill.");
			return false;
		}
		if (player.getSkillList().getSkillEntry(skillId).isStigma()) {
			PacketSendUtility.sendMessage(admin, "You can't remove stigma skill.");
			return false;
		}
		return true;
	}

	/**
	 * 删除单个技能或全部非烙印技能。
	 * Removes one skill or all non-stigma skills.
	 *
	 * admin
	 * target player
	 * @param skillId 技能 ID（0 表示全部） / skill id (0 means all)
	 * @param playerSkillList 技能列表（删除全部时使用） / skill list (used when removing all)
	 */
	public void apply(Player admin, Player player, int skillId, PlayerSkillList playerSkillList) {
		if (skillId != 0) {
			SkillLearnService.removeSkill(player, skillId);
			PacketSendUtility.sendMessage(admin, "You have successfully deleted the specified skill.");
		}
		else {
			for (PlayerSkillEntry skillEntry : playerSkillList.getAllSkills()) {
				if (!skillEntry.isStigma()) {
					SkillLearnService.removeSkill(player, skillEntry.getSkillId());
				}
			}

			PacketSendUtility.sendMessage(admin, "You have success delete All skills.");
		}

	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 *
	 * admin
	 * error message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "No parameters detected.\n" + "Please use //delskill <Player name> <all | skillId>\n" + "or use //delskill [target] <all | skillId>");
	}
}
