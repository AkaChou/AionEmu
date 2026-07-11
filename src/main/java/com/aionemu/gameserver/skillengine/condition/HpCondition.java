package com.aionemu.gameserver.skillengine.condition;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.SummonedObject;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * HP 条件：校验施法者当前生命是否高于阈值（可按技能等级与比例计算）。
 * HP condition: validates effector current HP is above a threshold (supports skill-level delta and ratio).
 *
 * @author Tomate
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HpCondition")
public class HpCondition extends Condition {

	@XmlAttribute(required = true)
	protected int value;
	@XmlAttribute
	protected int delta;
	@XmlAttribute
	protected boolean ratio;

	/**
	 * 校验技能环境是否满足本条件。
	 * Validates whether the skill environment satisfies this condition.
	 *
	 * @param skill 技能环境 / skill environment
	 * whether valid
	 */
	@Override
	public boolean validate(Skill skill) {
		// 仆从/图腾例外：允许施放最后技能后死亡 / exception for Servants, Totems to let them cast last skill and die
		if (skill.getEffector() instanceof SummonedObject) {
			return true;
		}
		int valueWithDelta = value + delta * skill.getSkillLevel();
		if (ratio) {
			valueWithDelta = (int) (valueWithDelta / 100f * skill.getEffector().getLifeStats().getMaxHp());
		}
		return skill.getEffector().getLifeStats().getCurrentHp() > valueWithDelta;
	}

	/**
	 * 获取模板中配置的 HP 阈值基值。
	 * Gets the base HP threshold configured in the template.
	 *
	 * HP base value
	 */
	public int getHpValue() {
		return value;
	}
}
