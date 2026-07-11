package com.aionemu.gameserver.model.atreian_bestiary;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 玩家 AB 条目，用于艾特里亚图鉴相关逻辑。
 * Player AB Entry for atreian bestiary logic.
 *
 * @author Ranastic
 */

public class PlayerABEntry extends ABEntry {
	private PersistentState persistentState;

	public PlayerABEntry(int id, int killCount, int level, int claimReward, PersistentState persistentState) {
		super(id, killCount, level, claimReward);
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
