package com.aionemu.gameserver.model.cp;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 玩家创造点条目，用于创造点相关逻辑。
 * Player CP Entry for cp logic.
 */

public class PlayerCPEntry extends CPEntry {

	private PersistentState persistentState;

	public PlayerCPEntry(int slot, int point, PersistentState persistentState) {
		super(slot, point);
		this.persistentState = persistentState;
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
