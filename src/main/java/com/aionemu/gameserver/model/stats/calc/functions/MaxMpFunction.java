package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * Max 魔法值函数，用于属性相关逻辑。
 * Max Mp Function for stats logic.
 */

class MaxMpFunction extends StatFunction {

	MaxMpFunction() {
		stat = StatEnum.MAXMP;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		float will = stat.getOwner().getGameStats().getWill().getCurrent();
		stat.setBase(Math.round(stat.getBase() * will / 100f));
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return 30;
	}
}
