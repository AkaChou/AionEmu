package com.aionemu.gameserver.model.drop;

import com.aionemu.gameserver.model.Race;

/**
 * 掉落修正器模型。
 * Drop Modifiers model.
 */

public class DropModifiers {

	private boolean dropNpcChest;
	private Race dropRace;
	private float boostDropRate;
	private Float reductionDropRate;

	/** Whether 掉落 npcchest / Whether drop npc chest */
	public boolean isDropNpcChest() {
		return dropNpcChest;
	}

	/** 设置 drop npc chest / Sets the drop npc chest */
	public void setDropNpcChest(boolean dropNpcChest) {
		this.dropNpcChest = dropNpcChest;
	}

	/** 获取掉落种族。 / Returns the drop race. */
	public Race getDropRace() {
		return dropRace;
	}

	/** 设置掉落种族。 / Sets the drop race. */
	public void setDropRace(Race dropRace) {
		this.dropRace = dropRace;
	}

	/** 设置 boost drop rate / Sets the boost drop rate */
	public void setBoostDropRate(float boostDropRate) {
		this.boostDropRate = boostDropRate;
	}

	/** 返回不小于零的完整普通掉落倍率。 / Returns the complete non-negative ordinary drop multiplier. */
	public float getPositiveBoostDropRate() {
		return Float.isFinite(boostDropRate) ? Math.max(0f, boostDropRate) : 0f;
	}

	/** 设置 reduction drop rate / Sets the reduction drop rate */
	public void setReductionDropRate(Float reductionDropRate) {
		this.reductionDropRate = reductionDropRate;
	}

	/** Calculate 掉落 chance / Calculate drop chance */
	public float calculateDropChance(float chance, boolean allowReductionDropRate) {
		if (allowReductionDropRate && reductionDropRate != null) {
			chance *= reductionDropRate;
		}
		return Math.min(chance * getPositiveBoostDropRate(), 100f);
	}

	/**
	 * 计算基纳数量。独立倍率覆盖基础份额，普通掉落倍率只贡献超过 1 倍的部分。
	 * Calculates Kinah: the dedicated rate scales the base share, while ordinary drop boosts contribute only above 1x.
	 */
	public long calculateKinahAmount(long baseKinah, float kinahRate) {
		if (!Float.isFinite(kinahRate) || kinahRate <= 0f) {
			throw new IllegalArgumentException("Kinah rate must be finite and greater than zero");
		}
		long dedicatedAmount = Math.round(baseKinah * (double) kinahRate);
		long ordinaryBoostAmount = Math.round(baseKinah * Math.max(0d, getPositiveBoostDropRate() - 1d));
		long total = dedicatedAmount > Long.MAX_VALUE - ordinaryBoostAmount
				? Long.MAX_VALUE
				: dedicatedAmount + ordinaryBoostAmount;
		return Math.max(1L, total);
	}
}
