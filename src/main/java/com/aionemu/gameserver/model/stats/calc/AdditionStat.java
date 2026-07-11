package com.aionemu.gameserver.model.stats.calc;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * Addition 属性，用于属性相关逻辑。
 * Addition Stat for stats logic.
 *
 * @author ATracer
 */
public class AdditionStat extends Stat2 {

	public AdditionStat(StatEnum stat, int base, Creature owner) {
		super(stat, base, owner);
	}

	public AdditionStat(StatEnum stat, int base, Creature owner, float bonusRate) {
		super(stat, base, owner, bonusRate);
	}

	public AdditionStat(StatEnum stat, float base, Creature owner) {
		super(stat, base, owner);
	}

	public AdditionStat(StatEnum stat, float base, Creature owner, float bonusRate) {
		super(stat, base, owner, bonusRate);
	}

	/** 添加 to base / Adds to base */
	@Override
	public final void addToBase(float base) {
		this.base += base;
	}

	/** Adds 到加成 / Adds to bonus */
	@Override
	public final void addToBonus(float bonus) {
		this.bonus += bonusRate * bonus;
	}

	/** 计算百分比 / Calculate percent */
	@Override
	public float calculatePercent(int delta) {
		return (100 + delta) / 100f;
	}
}
