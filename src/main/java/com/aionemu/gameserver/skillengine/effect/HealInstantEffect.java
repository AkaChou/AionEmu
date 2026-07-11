package com.aionemu.gameserver.skillengine.effect;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.HealType;

/**
 * 生命瞬时治疗效果：立即恢复目标 HP。
 * Instant HP heal effect: immediately restores the target's hit points.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HealInstantEffect")
public class HealInstantEffect extends AbstractHealEffect {

	/**
	 * 立即恢复目标生命。
	 * Immediately restores the target's HP.
	 */
	@Override
	public void applyEffect(Effect effect) {
		super.applyEffect(effect, HealType.HP);
	}

	/**
	 * 计算瞬时生命治疗。
	 * Calculates instant HP heal.
	 */
	@Override
	public void calculate(Effect effect) {
		super.calculate(effect, HealType.HP);
	}

	/**
	 * 返回当前生命值。
	 * Returns current HP.
	 */
	@Override
	protected int getCurrentStatValue(Effect effect) {
		return effect.getEffected().getLifeStats().getCurrentHp();
	}

	/**
	 * 返回最大生命值。
	 * Returns maximum HP.
	 */
	@Override
	protected int getMaxStatValue(Effect effect) {
		return effect.getEffected().getGameStats().getMaxHp().getCurrent();
	}
}
