package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * PvPAttackRatio 函数，用于属性相关逻辑。
 * Pv P Attack Ratio Function for stats logic.
 */

class PvPAttackRatioFunction extends DuplicateStatFunction {
	PvPAttackRatioFunction() {
		stat = StatEnum.PVP_ATTACK_RATIO;
	}
}
