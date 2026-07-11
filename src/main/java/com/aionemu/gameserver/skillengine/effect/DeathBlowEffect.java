package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.skillengine.action.DamageType;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 致命一击效果：以魔法伤害类型结算的瞬时伤害。
 * Death-blow effect: instant damage resolved as magical.
 */
public class DeathBlowEffect extends DamageEffect {

	/**
	 * 按魔法伤害计算。
	 * Calculates damage as magical.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void calculate(Effect effect) {
		super.calculate(effect, DamageType.MAGICAL);
	}
}
