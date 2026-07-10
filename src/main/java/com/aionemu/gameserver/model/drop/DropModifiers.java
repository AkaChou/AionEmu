package com.aionemu.gameserver.model.drop;

import com.aionemu.gameserver.model.Race;

public class DropModifiers {

	private boolean dropNpcChest;
	private Race dropRace;
	private float boostDropRate;
	private Float reductionDropRate;

	public boolean isDropNpcChest() {
		return dropNpcChest;
	}

	public void setDropNpcChest(boolean dropNpcChest) {
		this.dropNpcChest = dropNpcChest;
	}

	public Race getDropRace() {
		return dropRace;
	}

	public void setDropRace(Race dropRace) {
		this.dropRace = dropRace;
	}

	public void setBoostDropRate(float boostDropRate) {
		this.boostDropRate = boostDropRate;
	}

	public void setReductionDropRate(Float reductionDropRate) {
		this.reductionDropRate = reductionDropRate;
	}

	public float calculateDropChance(float chance, boolean allowReductionDropRate) {
		if (allowReductionDropRate && reductionDropRate != null) {
			chance *= reductionDropRate;
		}
		return Math.min(chance * boostDropRate, 100f);
	}
}
