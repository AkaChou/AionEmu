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

	/** 设置 reduction drop rate / Sets the reduction drop rate */
	public void setReductionDropRate(Float reductionDropRate) {
		this.reductionDropRate = reductionDropRate;
	}

	/** Calculate 掉落 chance / Calculate drop chance */
	public float calculateDropChance(float chance, boolean allowReductionDropRate) {
		if (allowReductionDropRate && reductionDropRate != null) {
			chance *= reductionDropRate;
		}
		return Math.min(chance * boostDropRate, 100f);
	}

	public float calculateScalingDropChance(float chance) {
		return chance == 100f ? 100f : calculateDropChance(chance, true);
	}
}
