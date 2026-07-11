package com.aionemu.gameserver.model.stats.calc;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 属性2模型。
 * Stat 2 model.
 *
 * @author ATracer
 */
public abstract class Stat2 {

	float bonusRate = 1f;
	float baseRate = 1f;
	float base;
	float bonus;
	float fixedBonusRate;
	private final Creature owner;
	protected final StatEnum stat;

	public Stat2(StatEnum stat, int base, Creature owner) {
		this(stat, base, owner, 1);
	}

	public Stat2(StatEnum stat, int base, Creature owner, float bonusRate) {
		this(stat, (float) base, owner, bonusRate);
	}

	public Stat2(StatEnum stat, float base, Creature owner) {
		this(stat, base, owner, 1);
	}

	public Stat2(StatEnum stat, float base, Creature owner, float bonusRate) {
		this.stat = stat;
		this.base = base;
		this.owner = owner;
		this.bonusRate = bonusRate;
	}

	/** 获取属性。 / Returns the stat. */
	public final StatEnum getStat() {
		return stat;
	}

	/** 获取基础。 / Returns the base. */
	public final int getBase() {
		return (int) (base * baseRate);
	}

	/** 返回 base without base rate / Returns the base without base rate */
	public final int getBaseWithoutBaseRate() {
		return (int) base;
	}

	/** 返回 exact base without base rate / Returns the exact base without base rate */
	public final float getExactBaseWithoutBaseRate() {
		return base;
	}

	/** 设置基础。 / Sets the base. */
	public final void setBase(float base) {
		this.base = base;
	}

	/** 获取基础比率。 / Returns the base rate. */
	public final float getBaseRate() {
		return baseRate;
	}

	/** 设置基础比率。 / Sets the base rate. */
	public final void setBaseRate(float baseRate) {
		this.baseRate = baseRate;
	}

	/** 添加 to base / Adds to base */
	public abstract void addToBase(float base);

	/** 获取加成。 / Returns the bonus. */
	public final int getBonus() {
		return (int) bonus;
	}

	/** 返回 exact bonus / Returns the exact bonus */
	public float getExactBonus() {
		return bonus;
	}

	/** 返回当前 / Returns the current */
	public final int getCurrent() {
		return (int) getExactCurrent();
	}

	/** 返回 exact current / Returns the exact current */
	public final float getExactCurrent() {
		return base * baseRate + bonus * bonusRate + base * fixedBonusRate;
	}

	/**
	 * 获取 ExactCurrentWithoutFixed 加成。
	 * Returns the exact current without fixed bonus.
	 */
	public final float getExactCurrentWithoutFixedBonus() {
		return base * baseRate + bonus * bonusRate;
	}

	/** 设置加成。 / Sets the bonus. */
	public final void setBonus(float bonus) {
		this.bonus = bonus;
	}

	/** 获取加成比率。 / Returns the bonus rate. */
	public final float getBonusRate() {
		return bonusRate;
	}

	/** 设置加成比率。 / Sets the bonus rate. */
	public final void setBonusRate(float bonusRate) {
		this.bonusRate = bonusRate;
	}

	/** Adds 到加成 / Adds to bonus */
	public abstract void addToBonus(float bonus);

	/** 设置 fixed bonus rate / Sets the fixed bonus rate */
	public void setFixedBonusRate(float fixedBonusRate) {
		this.fixedBonusRate = fixedBonusRate;
	}

	/** 返回 fixed bonus rate / Returns the fixed bonus rate */
	public float getFixedBonusRate() {
		return fixedBonusRate;
	}

	/** 计算百分比 / Calculate percent */
	public abstract float calculatePercent(int delta);

	/** 返回所有者 / Returns the owner*/
	public final Creature getOwner() {
		return owner;
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "[base=" + base + ", bonus=" + bonus + "]";
	}
}
