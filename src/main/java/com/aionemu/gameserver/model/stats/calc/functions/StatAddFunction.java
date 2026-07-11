package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 属性 Add 函数模型。
 * Stat Add Function model.
 *
 * @author ATracer
 */
public class StatAddFunction extends StatFunction {

	public StatAddFunction() {
	}

	public StatAddFunction(StatEnum name, int value, boolean bonus) {
		super(name, value, bonus);
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		if (isBonus()) {
			stat.addToBonus(getValue());
		} else {
			stat.addToBase(getValue());
		}
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return isBonus() ? 50 : 30;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "StatAddFunction [" + super.toString() + "]";
	}
}
