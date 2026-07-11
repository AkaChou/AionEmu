package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 属性 Set 函数模型。
 * Stat Set Function model.
 *
 * @author ATracer
 */
public class StatSetFunction extends StatFunction {

	public StatSetFunction() {
	}

	public StatSetFunction(StatEnum name, int value, boolean bonus) {
		super(name, value, bonus);
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		if (isBonus()) {
			stat.setBonus(getValue());
		} else {
			stat.setBase(getValue());
		}
	}

	/** 返回 priority / Returns the priority */
	@Override
	public final int getPriority() {
		return 10;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "StatSetFunction [" + super.toString() + "]";
	}
}
