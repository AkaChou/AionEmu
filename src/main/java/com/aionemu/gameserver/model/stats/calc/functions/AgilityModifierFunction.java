package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * Agility 修正器函数，用于属性相关逻辑。
 * Agility Modifier Function for stats logic.
 */

class AgilityModifierFunction extends StatFunction {

	private float modifier;

	AgilityModifierFunction(StatEnum stat, float modifier) {
		this.stat = stat;
		this.modifier = modifier;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		float agility = stat.getOwner().getGameStats().getAgility().getCurrent();
		stat.setBase(Math.round(stat.getBase() + stat.getBase() * (agility - 100) * modifier / 100f));
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return 30;
	}
}
