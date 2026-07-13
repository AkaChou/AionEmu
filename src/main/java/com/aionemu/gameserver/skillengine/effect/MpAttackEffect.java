package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 魔法值持续削减效果：周期扣除目标 MP。
 * MP drain-over-time effect: periodically reduces the target's magic points.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MpAttackEffect")
public class MpAttackEffect extends AbstractOverTimeEffect {

	/**
	 * 周期扣除目标魔法值。
	 * Periodically drains the target's MP.
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		int maxMP = effect.getEffected().getLifeStats().getMaxMp();
		int newValue = calculateValue(effect.getSkillLevel());
		// 支持百分比数值 / Support for values in percentage
		if (percent) {
			newValue = maxMP * newValue / 100;
		}
		effect.getEffected().getLifeStats().reduceMp(newValue);
	}
}
