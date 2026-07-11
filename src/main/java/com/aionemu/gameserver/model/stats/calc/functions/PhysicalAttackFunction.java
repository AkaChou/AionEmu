package com.aionemu.gameserver.model.stats.calc.functions;

import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * PhysicalAttack 函数，用于属性相关逻辑。
 * Physical Attack Function for stats logic.
 */

class PhysicalAttackFunction extends StatFunction {

	PhysicalAttackFunction() {
		stat = StatEnum.PHYSICAL_ATTACK;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		float power = stat.getOwner().getGameStats().getPower().getCurrent();
		if (stat.getOwner() instanceof Player
				&& ArrayUtils.contains(calculationTypes, CalculationType.SKILL)
				&& ArrayUtils.contains(calculationTypes, CalculationType.DUAL_WIELD)) {
			power = power > 100 ? Rnd.get(100, (int) power) : Rnd.get((int) power, 100);
		}
		stat.setBaseRate(power / 100f);
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return 30;
	}
}
