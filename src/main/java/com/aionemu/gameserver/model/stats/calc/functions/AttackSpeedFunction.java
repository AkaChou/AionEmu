package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * AttackSpeed 函数，用于属性相关逻辑。
 * Attack Speed Function for stats logic.
 */

class AttackSpeedFunction extends DuplicateStatFunction {

	AttackSpeedFunction() {
		stat = StatEnum.ATTACK_SPEED;
	}
}
