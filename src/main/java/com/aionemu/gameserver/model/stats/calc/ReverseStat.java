package com.aionemu.gameserver.model.stats.calc;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * Reverse 属性，用于属性相关逻辑。
 * Reverse Stat for stats logic.
 *
 * @author ATracer
 */
public class ReverseStat extends Stat2 {

	public ReverseStat(StatEnum stat, int base, Creature owner) {
		super(stat, base, owner);
	}

	public ReverseStat(StatEnum stat, int base, Creature owner, float bonusRate) {
		super(stat, base, owner, bonusRate);
	}

	public ReverseStat(StatEnum stat, float base, Creature owner) {
		super(stat, base, owner);
	}

	public ReverseStat(StatEnum stat, float base, Creature owner, float bonusRate) {
		super(stat, base, owner, bonusRate);
	}

	/** 添加 to base / Adds to base */
	@Override
	public void addToBase(float base) {
		this.base -= base;
		if (this.base < 0) {
			this.base = 0;
		}
	}

	/** Adds 到加成 / Adds to bonus */
	@Override
	public void addToBonus(float bonus) {
		this.bonus -= bonusRate * bonus;
	}

	/** 计算百分比 / Calculate percent */
	@Override
	public float calculatePercent(int delta) {
		return Math.max(0, (100 - delta) / 100f);
	}
}
