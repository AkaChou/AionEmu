package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;

/**
 * 驱散效果基类：按类别与目标槽位移除受影响者身上的效果。
 * Base class for dispel effects: removes effects on the target by category and slot.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractDispelEffect")
public class AbstractDispelEffect extends EffectTemplate {

	@XmlAttribute
	protected int dpower;
	@XmlAttribute
	protected int power;
	@XmlAttribute(name = "dispel_level_delta")
	protected int dispelLevelDelta;
	@XmlAttribute(name = "dispel_level")
	protected int dispelLevel;

	/**
	 * 无参应用入口（子类覆盖并指定驱散类别）。
	 * No-arg apply entry (subclasses override with a concrete dispel category).
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void applyEffect(Effect effect) {
	}

	/**
	 * 按驱散类别与目标槽位移除效果，驱散层数与强度随技能等级缩放。
	 * Removes effects by dispel category and target slot; count and power scale with skill level.
	 *
	 * @param effect 运行时效果 / runtime effect
	 * @param type 驱散类别 / dispel category
	 * @param slot 目标槽位 / target slot
	 */
	public void applyEffect(Effect effect, DispelCategoryType type, SkillTargetSlot slot) {
		int skillLevel = effect.getSkillLevel();
		int count = value + delta * skillLevel;
		int finalPower = power + dpower * skillLevel;
		int finalDispelLevel = dispelLevel + dispelLevelDelta * skillLevel;

		effect.getEffected().getEffectController().removeEffectByDispelCat(type, slot, count, finalDispelLevel, finalPower);
	}
}
