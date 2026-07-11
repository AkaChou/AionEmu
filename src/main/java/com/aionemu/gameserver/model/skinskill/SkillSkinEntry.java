package com.aionemu.gameserver.model.skinskill;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.SkillSkinTemplate;

/**
 * 技能外观条目。
 * Skill Skin Entry model.
 *
 * @author Rinzler (Encom)
 */
public abstract class SkillSkinEntry {

	protected final int skinId;
	protected int skillLevel;

	SkillSkinEntry(int skinId, int skillLevel) {
		this.skinId = skinId;
		this.skillLevel = skillLevel;
	}

	/** 返回皮肤 ID / Returns the skin id */
	public final int getSkinId() {
		return skinId;
	}

	/** 获取技能等级。 / Returns the skill level. */
	public final int getSkillLevel() {
		return skillLevel;
	}

	/** 获取技能名称。 / Returns the skill name. */
	public final String getSkillName() {
		return DataManager.SKILL_SKIN_DATA.getSkillSkinTemplate(getSkinId()).getName();
	}

	/** 设置技能等级 / Sets the skill lvl */
	public void setSkillLvl(int skillLevel) {
		this.skillLevel = skillLevel;
	}

	/** 获取技能外观模板。 / Returns the skill skin template. */
	public final SkillSkinTemplate getSkillSkinTemplate() {
		return DataManager.SKILL_SKIN_DATA.getSkillSkinTemplate(getSkinId());
	}
}
