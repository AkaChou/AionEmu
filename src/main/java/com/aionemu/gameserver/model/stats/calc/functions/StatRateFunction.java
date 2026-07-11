package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 属性比率函数模型。
 * Stat Rate Function model.
 *
 * @author ATracer
 */
public class StatRateFunction extends StatFunction {

	public StatRateFunction() {
	}

	public StatRateFunction(StatEnum name, int value, boolean bonus) {
		super(name, value, bonus);
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		if (isBonus()) {
			stat.addToBonus((int) (stat.getBase() * getValue() / 100f));
		} else {
			stat.setBase((int) (stat.getBase() * stat.calculatePercent(getValue())));
		}
	}

	/** 返回 priority / Returns the priority */
	@Override
	public final int getPriority() {
		return isBonus() ? 40 : 20;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "StatRateFunction [" + super.toString() + "]";
	}
}
