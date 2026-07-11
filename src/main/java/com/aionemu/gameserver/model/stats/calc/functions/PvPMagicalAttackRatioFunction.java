package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * PvPMagicalAttackRatio 函数，用于属性相关逻辑。
 * Pv P Magical Attack Ratio Function for stats logic.
 */

public class PvPMagicalAttackRatioFunction extends DuplicateStatFunction {

	PvPMagicalAttackRatioFunction() {
		stat = StatEnum.PVP_ATTACK_RATIO_MAGICAL;
	}
}
