package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;

/**
 * 属性 Sub 函数模型。
 * Stat Sub Function model.
 *
 * @author ATracer
 */
public class StatSubFunction extends StatFunction {

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		if (isBonus()) {
			stat.addToBonus(-getValue());
		} else {
			stat.addToBase(-getValue());
		}
	}

	/** 返回 priority / Returns the priority */
	@Override
	public final int getPriority() {
		return isBonus() ? 50 : 30;
	}
}
