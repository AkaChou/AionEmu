package com.aionemu.gameserver.model.stats.calc;

import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;

/**
 * 属性 Condition 接口。
 * Stat Condition interface.
 *
 * @author ATracer
 */
public interface StatCondition {

	/**
	 * 校验该函数是否应应用于属性。 / Validate that function should be applied to the stat.
	 */
	boolean validate(Stat2 stat, IStatFunction statFunction);
}
