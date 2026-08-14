package com.aionemu.gameserver.network.aion.gmhandler;

import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * GM 指令：按技能栈名删除玩家非印记技能。
 * GM command handler that removes non-stigma skills matching a skill stack name.
 *
 * @author Kill3r
 */
public class CmdDeleteSkill extends AbstractGMHandler {

	/**
	 * 创建处理器并立即执行删除技能。
	 * Creates the handler and immediately runs the delete-skill logic.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 技能名称/栈名（可含 _G 等级后缀） / skill name/stack (may include _G rank suffix)
	 */
	public CmdDeleteSkill(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 规范化技能栈名后，移除管理员身上匹配的非印记技能。
	 * Normalizes the skill stack name and removes matching non-stigma skills from the admin.
	 */
	public void run() {
		String skillName = params;
		PlayerSkillList playerSkillList = admin.getSkillList();
		List<SkillTemplate> skill = DataManager.SKILL_DATA.getSkillTemplates();

		if (skillName.contains("_G")) {
			skillName = "SKILL_" + params;
			if (checkLevel(params) == 1) {
				skillName = skillName.replaceAll("_G1", "");
			} else if (checkLevel(params) == 2) {
				skillName = skillName.replaceAll("_G2", "");
			} else if (checkLevel(params) == 3) {
				skillName = skillName.replaceAll("_G3", "");
			} else if (checkLevel(params) == 4) {
				skillName = skillName.replaceAll("_G4", "");
			} else if (checkLevel(params) == 5) {
				skillName = skillName.replaceAll("_G5", "");
			} else if (checkLevel(params) == 6) {
				skillName = skillName.replaceAll("_G6", "");
			} else if (checkLevel(params) == 7) {
				skillName = skillName.replaceAll("_G7", "");
			} else if (checkLevel(params) == 8) {
				skillName = skillName.replaceAll("_G8", "");
			} else if (checkLevel(params) == 9) {
				skillName = skillName.replaceAll("_G9", "");
			} else if (checkLevel(params) == 10) {
				skillName = skillName.replaceAll("_G10", "");
			}
		} else {
			skillName = params;
		}

		for (SkillTemplate s : skill) {
			if (s.getStack().equalsIgnoreCase(skillName)) {
				for (PlayerSkillEntry skillEntry : playerSkillList.getAllSkills()) {
					if (!skillEntry.isStigma()) {
						SkillLearnService.removeSkill(admin, skillEntry.getSkillId());
					}
				}
			}
		}

	}

	/**
	 * 从技能名后缀解析 G1–G10 等级序号。
	 * Parses the G1–G10 rank suffix from a skill name.
	 *
	 * @param string 技能名 / skill name
	 * @return 等级序号，默认 1 / rank number, default 1
	 */
	private int checkLevel(String string) {
		if (string.endsWith("G1")) {
			return 1;
		} else if (string.endsWith("G2")) {
			return 2;
		} else if (string.endsWith("G3")) {
			return 3;
		} else if (string.endsWith("G4")) {
			return 4;
		} else if (string.endsWith("G5")) {
			return 5;
		} else if (string.endsWith("G6")) {
			return 6;
		} else if (string.endsWith("G7")) {
			return 7;
		} else {
			return 1;
		}
	}
}
