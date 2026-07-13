package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 魔法值瞬时削减效果：立即扣除目标 MP。
 * Instant MP attack effect: immediately reduces the target's magic points.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MpAttackInstantEffect")
public class MpAttackInstantEffect extends EffectTemplate {

	@XmlAttribute
	protected boolean percent;

	/**
	 * 立即扣除目标魔法值。
	 * Immediately drains the target's MP.
	 */
	@Override
	public void applyEffect(Effect effect) {
		int maxMP = effect.getEffected().getLifeStats().getMaxMp();
		int newValue = calculateValue(effect.getSkillLevel());
		// 支持百分比数值 / Support for values in percentage
		if (percent) {
			newValue = maxMP * newValue / 100;
		}
		effect.getEffected().getLifeStats().reduceMp(newValue);
	}
}
