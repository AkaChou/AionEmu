package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 免除死亡惩罚效果：标记效果实例，死亡时不施加惩罚。
 * No death-penalty effect: marks the effect so death applies no penalty.
 */
public class NoDeathPenaltyEffect extends BuffEffect {

	/**
	 * 标记本效果计算成功。
	 * Marks this effect calculation as successful.
	 */
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	/**
	 * 设置免除死亡惩罚标记。
	 * Sets the no-death-penalty flag.
	 */
	public void startEffect(Effect effect) {
		effect.setNoDeathPenalty(true);
	}
}
