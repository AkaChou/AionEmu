package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * PvPPhysicalDefendRatio 函数，用于属性相关逻辑。
 * Pv P Physical Defend Ratio Function for stats logic.
 */

public class PvPPhysicalDefendRatioFunction extends DuplicateStatFunction {

	PvPPhysicalDefendRatioFunction() {
		stat = StatEnum.PVP_DEFEND_RATIO_PHYSICAL;
	}
}
