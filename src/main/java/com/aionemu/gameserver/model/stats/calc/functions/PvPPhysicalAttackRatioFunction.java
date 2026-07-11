package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * PvPPhysicalAttackRatio 函数，用于属性相关逻辑。
 * Pv P Physical Attack Ratio Function for stats logic.
 */

public class PvPPhysicalAttackRatioFunction extends DuplicateStatFunction {

	PvPPhysicalAttackRatioFunction() {
		stat = StatEnum.PVP_ATTACK_RATIO_PHYSICAL;
	}
}
