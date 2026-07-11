package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 魔法值持续恢复效果：周期回复目标 MP。
 * MP heal-over-time effect: periodically restores the target's magic points.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MPHealEffect")
public class MPHealEffect extends HealOverTimeEffect {

	/**
	 * 计算 MP 持续恢复。
	 * Calculates MP heal-over-time.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.MP);
	}

	/**
	 * 周期恢复目标魔法值。
	 * Periodically restores the target's MP.
	 */
	@Override
	public void onPeriodicAction(Effect effect) {
		super.onPeriodicAction(effect, HealType.MP);
	}

	/**
	 * 返回当前魔法值。
	 * Returns current MP.
	 */
	@Override
	protected int getCurrentStatValue(Effect effect) {
		return effect.getEffected().getLifeStats().getCurrentMp();
	}

	/**
	 * 返回最大魔法值。
	 * Returns maximum MP.
	 */
	@Override
	protected int getMaxStatValue(Effect effect) {
		return effect.getEffected().getGameStats().getMaxMp().getCurrent();
	}
}
