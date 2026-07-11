package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 子效果配置：在主效果成功时按概率触发额外技能。
 * Sub-effect config: triggers an extra skill by chance when the main effect succeeds.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SubEffect")
public class SubEffect {

	@XmlAttribute(name = "skill_id", required = true)
	private int skillId;
	@XmlAttribute
	private int chance = 100;

	@XmlAttribute(name = "addeffect")
	private boolean addEffect = false;

	/**
	 * 获取子效果技能 ID。
	 * Returns the sub-effect skill id.
	 *
	 * skill id
	 */
	public int getSkillId() {
		return skillId;
	}

	/**
	 * 获取触发概率（0–100）。
	 * Returns the trigger chance (0–100).
	 *
	 * chance
	 */
	public int getChance() {
		return chance;
	}

	/**
	 * 是否按印记爆发层数作为技能等级。
	 * Whether skill level is taken from signet burst count.
	 *
	 * @return true 表示使用印记层数 / true if using signet burst count
	 */
	public boolean isAddEffect() {
		return addEffect;
	}
}
