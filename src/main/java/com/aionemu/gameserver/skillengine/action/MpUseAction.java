package com.aionemu.gameserver.skillengine.action;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;

/**
 * MP 消耗动作：施法时从施法者扣除魔法值（可受技能消耗加成修正）。
 * MP cost action: reduces caster MP on cast (may apply skill-cost boost).
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MpUseAction")
public class MpUseAction extends Action {

	/**
	 * 基础 MP 消耗值。
	 * Base MP cost value.
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
	 * 是否按最大 MP 百分比计算。
	 * Whether the cost is a percentage of max MP.
	 */
	@XmlAttribute
	protected boolean ratio;

	/**
	 * 按等级、比例与技能消耗加成计算后扣除 MP。
	 * Reduces MP after level delta, optional ratio, and skill-cost boost.
	 *
	 * @param skill 当前技能上下文 / current skill context
	 */
	@Override
	public void act(Skill skill) {
		Creature effector = skill.getEffector();
		int valueWithDelta = value + delta * skill.getSkillLevel();
		if (ratio) {
			valueWithDelta = (int) ((skill.getEffector().getLifeStats().getMaxMp() * valueWithDelta) / 100);
		}
		int changeMpPercent = skill.getBoostSkillCost();
		if (changeMpPercent != 0) {
 // changeMpPercent 为负 / changeMpPercent is negative
			valueWithDelta = valueWithDelta - ((valueWithDelta / ((100 / changeMpPercent))));
		}
		effector.getLifeStats().reduceMp(valueWithDelta);
	}
}
