package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * Max 生命值函数，用于属性相关逻辑。
 * Max Hp Function for stats logic.
 */

class MaxHpFunction extends StatFunction {

	MaxHpFunction() {
		stat = StatEnum.MAXHP;
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		float health = stat.getOwner().getGameStats().getHealth().getCurrent();
		stat.setBase(Math.round(stat.getBase() * health / 100f));
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return 30;
	}
}
