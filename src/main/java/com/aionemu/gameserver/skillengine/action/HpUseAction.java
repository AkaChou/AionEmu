package com.aionemu.gameserver.skillengine.action;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * HP 消耗动作：施法时从施法者扣除生命值。
 * HP cost action: reduces the caster's HP on cast.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HpUseAction")
public class HpUseAction extends Action {

	/**
	 * 基础 HP 消耗值。
	 * Base HP cost value.
	 */
	@XmlAttribute(required = true)
	protected int value;

	/**
	 * 每技能等级额外增量。
	 * Extra amount per skill level.
	 */
	@XmlAttribute
	protected int delta;

	/**
	 * 是否按最大 HP 百分比计算。
	 * Whether the cost is a percentage of max HP.
	 */
	@XmlAttribute
	protected boolean ratio;

	/**
	 * 按等级与比例计算后扣除施法者 HP。
	 * Reduces caster HP after applying level delta and optional ratio.
	 *
	 * @param skill 当前技能上下文 / current skill context
	 */
	@Override
	public void act(Skill skill) {
		Creature effector = skill.getEffector();
		int valueWithDelta = value + delta * skill.getSkillLevel();
		if (ratio) {
			valueWithDelta = (int) (valueWithDelta / 100f * skill.getEffector().getLifeStats().getMaxHp());
		}
		effector.getLifeStats().reduceHp(valueWithDelta, effector);
	}
}
