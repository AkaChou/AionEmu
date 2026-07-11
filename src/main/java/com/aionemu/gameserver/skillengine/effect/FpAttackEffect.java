package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 飞行点持续削减效果：周期扣除目标 FP。
 * FP drain-over-time effect: periodically reduces the target's flight points.
 *
 * @author Sippolo
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FpAttackEffect")
public class FpAttackEffect extends AbstractOverTimeEffect {

	/**
	 * 计算 FP 持续削减是否生效。
	 * Calculates whether FP DoT applies.
	 */
	@Override
	public void calculate(Effect effect) {
		// 仅玩家有飞行时间 / Only players have FP
		if (effect.getEffected() instanceof Player) {
			super.calculate(effect, null, null);
		}
	}

	/**
	 * 周期扣除目标飞行点。
	 * Periodically drains the target's flight points.
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		Player effected = (Player) effect.getEffected();
		int maxFP = effected.getLifeStats().getMaxFp();
		int newValue = value;
		// 支持百分比数值 / Support for values in percentage
		if (percent) {
			newValue = (int) ((maxFP * value) / 100);
		}
		effected.getLifeStats().reduceFp(newValue);
	}
}
