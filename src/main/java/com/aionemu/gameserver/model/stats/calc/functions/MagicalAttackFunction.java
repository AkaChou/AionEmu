package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * MagicalAttack 函数，用于属性相关逻辑。
 * Magical Attack Function for stats logic.
 */

class MagicalAttackFunction extends StatFunction {

	MagicalAttackFunction() {
		stat = StatEnum.MAGICAL_ATTACK;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		float knowledge = stat.getOwner().getGameStats().getKnowledge().getCurrent();
		stat.setBaseRate(knowledge / 100.0F);
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return 30;
	}
}
