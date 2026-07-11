package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * 属性函数接口。
 * Stat Function interface.
 *
 * @author ATracer
 */
public interface IStatFunction extends Comparable<IStatFunction> {

	StatEnum getName();

	boolean isBonus();

	int getPriority();

	int getValue();

	boolean validate(Stat2 stat, IStatFunction statFunction);

	void apply(Stat2 stat);

	default void apply(Stat2 stat, CalculationType... calculationTypes) {
		apply(stat);
	}

	StatOwner getOwner();

	boolean hasConditions();
}
