package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * PDef 函数，用于属性相关逻辑。
 * P Def Function for stats logic.
 */

class PDefFunction extends StatFunction {

	PDefFunction() {
		stat = StatEnum.PHYSICAL_DEFENSE;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		if (stat.getOwner().isInFlyingState()) {
			stat.setBonus(stat.getBonus() - (stat.getBase() / 2));
		}
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return 60;
	}
}
