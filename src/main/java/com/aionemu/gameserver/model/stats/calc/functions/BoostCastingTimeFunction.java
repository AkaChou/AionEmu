package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * BoostCastingTime 函数，用于属性相关逻辑。
 * Boost Casting Time Function for stats logic.
 */

class BoostCastingTimeFunction extends DuplicateStatFunction {

	BoostCastingTimeFunction() {
		stat = StatEnum.BOOST_CASTING_TIME;
	}
}
