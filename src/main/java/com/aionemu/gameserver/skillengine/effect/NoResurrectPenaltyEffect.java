package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 免除复活惩罚效果：标记效果实例，复活时不施加惩罚。
 * No resurrect-penalty effect: marks the effect so resurrection applies no penalty.
 */
public class NoResurrectPenaltyEffect extends BuffEffect {

	/**
	 * 标记本效果计算成功。
	 * Marks this effect calculation as successful.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	/**
	 * 设置免除复活惩罚标记。
	 * Sets the no-resurrect-penalty flag.
	 *
	 * @param effect 运行时效果 / runtime effect
	 */
	public void startEffect(Effect effect) {
		effect.setNoResurrectPenalty(true);
	}
}
