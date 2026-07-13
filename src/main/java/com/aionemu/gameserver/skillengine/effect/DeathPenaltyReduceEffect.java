package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 死亡惩罚减免效果：作为 Buff 壳，用于降低死亡惩罚。
 * Death-penalty reduce effect: buff shell that reduces death penalty.
 *
 * @author Rinzler (Encom)
 */
public class DeathPenaltyReduceEffect extends BuffEffect {

	@Override
	public void calculate(Effect effect) {
		effect.setDeathPenaltyReduce(true);
		effect.addSucessEffect(this);
	}
}
