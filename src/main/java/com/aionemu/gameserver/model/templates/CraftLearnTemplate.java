package com.aionemu.gameserver.model.templates;

/**
 * 制作学习模板（静态数据/XML）。
 * Craft learn template (static data / XML).
 */

public class CraftLearnTemplate {

	private int skillId;
	private boolean isCraftSkill;

	/** 是否为制作技能。 / Whether craft skill. */
	public boolean isCraftSkill() {
		return isCraftSkill;
	}

	public CraftLearnTemplate(int skillId, boolean isCraftSkill, String skillName) {
		this.skillId = skillId;
		this.isCraftSkill = isCraftSkill;
	}

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return skillId;
	}
}
