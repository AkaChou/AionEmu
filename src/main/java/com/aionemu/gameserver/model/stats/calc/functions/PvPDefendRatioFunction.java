package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * PvPDefendRatio 函数，用于属性相关逻辑。
 * Pv P Defend Ratio Function for stats logic.
 */

class PvPDefendRatioFunction extends DuplicateStatFunction {
	PvPDefendRatioFunction() {
		stat = StatEnum.PVP_DEFEND_RATIO;
	}
}
