package com.aionemu.gameserver.model.shugo_sweep;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 玩家清扫条目，用于术古清扫相关逻辑。
 * Player Sweep Entry for shugo sweep logic.
 */

public class PlayerSweepEntry extends SweepEntry {

	private PersistentState persistentState;

	public PlayerSweepEntry(int id, boolean isReward, PersistentState persistentState) {
		super(id, isReward);
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case DELETED:
			if (this.persistentState == PersistentState.NEW) {
				this.persistentState = PersistentState.NOACTION;
			} else {
				this.persistentState = PersistentState.DELETED;
			}
			break;
		case UPDATE_REQUIRED:
			if (this.persistentState != PersistentState.NEW) {
				this.persistentState = PersistentState.UPDATE_REQUIRED;
			}
			break;
		case NOACTION:
			break;
		default:
			this.persistentState = persistentState;
		}
	}
}
