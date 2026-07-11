package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * PvPMagicalDefendRatio 函数，用于属性相关逻辑。
 * Pv P Magical Defend Ratio Function for stats logic.
 */

public class PvPMagicalDefendRatioFunction extends DuplicateStatFunction {

	PvPMagicalDefendRatioFunction() {
		stat = StatEnum.PVP_DEFEND_RATIO_MAGICAL;
	}
}
