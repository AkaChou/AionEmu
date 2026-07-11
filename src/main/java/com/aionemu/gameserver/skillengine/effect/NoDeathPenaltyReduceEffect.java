package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 死亡惩罚减免效果：标记效果实例，降低死亡惩罚。
 * Death-penalty reduce effect: marks the effect to lessen death penalties.
 *
 * @author Rinzler (Encom)
 */
public class NoDeathPenaltyReduceEffect extends BuffEffect {

	/**
	 * 标记本效果计算成功。
	 * Marks this effect calculation as successful.
	 */
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}

	/**
	 * 设置死亡惩罚减免标记。
	 * Sets the death-penalty-reduce flag.
	 */
	public void startEffect(Effect effect) {
		effect.setNoDeathPenaltyReduce(true);
	}
}
