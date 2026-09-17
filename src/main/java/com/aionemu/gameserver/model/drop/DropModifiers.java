package com.aionemu.gameserver.model.drop;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 掉落修正器模型。
 * Drop Modifiers model.
 */

@Getter
@Setter
public class DropModifiers {

	/** 是否掉落 NPC 宝箱 / Whether to drop the NPC chest */
	private boolean dropNpcChest;
	/** 获取掉落种族。 / Returns the drop race. */
	private Race dropRace;
	/** 设置掉落倍率加成 / Sets the boost drop rate */
	private float boostDropRate;
	/** 设置掉率衰减倍率 / Sets the reduction drop rate */
	private Float reductionDropRate;

	/** 返回不小于零的完整普通掉落倍率。 / Returns the complete non-negative ordinary drop multiplier. */
	public float getPositiveBoostDropRate() {
		return Float.isFinite(boostDropRate) ? Math.max(0f, boostDropRate) : 0f;
	}

	/** 计算掉落概率 / Calculate drop chance */
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
