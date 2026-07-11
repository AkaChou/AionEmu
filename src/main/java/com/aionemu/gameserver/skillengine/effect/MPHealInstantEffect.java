package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 魔法值瞬时恢复效果：立即回复目标 MP。
 * Instant MP heal effect: immediately restores the target's magic points.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MPHealInstantEffect")
public class MPHealInstantEffect extends AbstractHealEffect {

	/**
	 * 计算瞬时 MP 恢复。
	 * Calculates instant MP heal.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.MP);
	}

	/**
	 * 立即恢复目标魔法值。
	 * Immediately restores the target's MP.
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, HealType.MP);
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
