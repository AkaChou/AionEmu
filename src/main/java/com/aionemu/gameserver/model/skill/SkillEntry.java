package com.aionemu.gameserver.model.skill;

import java.sql.Timestamp;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

/**
 * 技能条目。
 * Skill Entry model.
 *
 * @author ATracer
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class SkillEntry {

	protected final int skillId;
	protected int skillLevel;
	/** 设置 skin id / Sets the skin id */
	protected int skinId;
	protected Timestamp activeSkinTime;
	protected int expireTime;
	/**
	 * @return 是否激活 / Whether activated
	 */
	protected boolean isActivated;

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

	/** 设置 skin expire time / Sets the skin expire time */
	public void setSkinExpireTime(int expireTime) {
		this.expireTime = expireTime;
	}
}
