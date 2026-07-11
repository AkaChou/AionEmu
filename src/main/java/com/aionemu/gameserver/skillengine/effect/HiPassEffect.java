package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 高等通过效果：标记效果实例，使相关判定跳过部分限制。
 * Hi-pass effect: marks the effect so related checks bypass some restrictions.
 */
public class HiPassEffect extends BuffEffect {

	/**
	 * 标记本效果计算成功。
	 * Marks this effect calculation as successful.
	 */
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	/**
	 * 设置 HiPass 标记。
	 * Sets the HiPass flag on the effect.
	 */
	public void startEffect(Effect effect) {
		effect.setHiPass(true);
	}
}
