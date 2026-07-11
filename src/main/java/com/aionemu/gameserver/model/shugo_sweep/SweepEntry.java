package com.aionemu.gameserver.model.shugo_sweep;

import lombok.Getter;

/**
 * 清扫条目，用于术古清扫相关逻辑。
 * Sweep Entry for shugo sweep logic.
 */
@Getter
public class SweepEntry {

	private int id;
	private boolean isReward;

	public SweepEntry(int id, boolean isReward) {
		this.id = id;
		this.isReward = isReward;
	}

}
