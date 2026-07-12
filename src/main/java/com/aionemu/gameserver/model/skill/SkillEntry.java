package com.aionemu.gameserver.model.skill;

import java.sql.Timestamp;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/**
 * 技能条目。
 * Skill Entry model.
 *
 * @author ATracer
 */
public abstract class SkillEntry {

	protected final int skillId;
	protected int skillLevel;
	protected int skinId;
	protected Timestamp activeSkinTime;
	protected int expireTime;
	protected boolean isActivated;

	SkillEntry(int skillId, int skillLevel, int skinId, Timestamp activeSkinTime, int expireTime, boolean isActivated) {
		this.skillId = skillId;
		this.skillLevel = skillLevel;
		this.skinId = skinId;
		this.activeSkinTime = activeSkinTime;
		this.expireTime = expireTime;
		this.isActivated = isActivated;
	}

	/** 返回技能 ID / Returns the skill id */
	public final int getSkillId() {
		return skillId;
	}

	/** 获取技能等级。 / Returns the skill level. */
	public final int getSkillLevel() {
		return skillLevel;
	}

	/** 返回皮肤 ID / Returns the skin id */
	public final int getSkinId() {
		return skinId;
	}

	/** 返回皮肤当前时间 / Returns the skin active time */
	public final Timestamp getSkinActiveTime() {
		return activeSkinTime;
	}

	/** 设置 skin active time / Sets the skin active time */
	public void setSkinActiveTime(Timestamp activeSkinTime) {
		this.activeSkinTime = activeSkinTime;
	}

	/** 返回皮肤过期时间 / Returns the skin expire time */
	public final int getSkinExpireTime() {
		return expireTime;
	}

	/** 获取技能名称。 / Returns the skill name. */
	public final String getSkillName() {
		return DataManager.SKILL_DATA.getSkillTemplate(getSkillId()).getName();
	}

	/** 设置技能等级 / Sets the skill lvl */
	public void setSkillLvl(int skillLevel) {
		this.skillLevel = skillLevel;
	}

	/** 获取技能模板。 / Returns the skill template. */
	public final SkillTemplate getSkillTemplate() {
		return DataManager.SKILL_DATA.getSkillTemplate(getSkillId());
	}

	/** 设置 skin id / Sets the skin id */
	public void setSkinId(int skinId) {
		this.skinId = skinId;
	}

	/** 设置 skin expire time / Sets the skin expire time */
	public void setSkinExpireTime(int expireTime) {
		this.expireTime = expireTime;
	}

	/**
	 * @return Whether activated
	 */
	public boolean isActivated() {
		return isActivated;
	}

	/** 设置 activated / Sets the activated */
	public void setActivated(boolean activated) {
		this.isActivated = activated;
	}
}
