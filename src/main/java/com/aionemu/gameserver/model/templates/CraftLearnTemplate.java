package com.aionemu.gameserver.model.templates;

import lombok.Getter;

/**
 * 制作学习模板（静态数据/XML）。
 * Craft learn template (static data / XML).
 */

@Getter
public class CraftLearnTemplate {

	/** 返回技能 ID / Returns the skill id */
	private final int skillId;
	/** 是否为制作技能。 / Whether craft skill. */
	private final boolean isCraftSkill;

	public CraftLearnTemplate(int skillId, boolean isCraftSkill, String skillName) {
		this.skillId = skillId;
		this.isCraftSkill = isCraftSkill;
	}
}
