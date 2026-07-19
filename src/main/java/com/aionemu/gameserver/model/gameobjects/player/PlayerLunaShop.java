package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 玩家月华 Shop 游戏对象。
 * Player Luna Shop game object.
 */

public class PlayerLunaShop {
	private PersistentState persistentState;

	private boolean freeChest;

	public PlayerLunaShop(boolean freeChest) {
		this.freeChest = freeChest;
		this.persistentState = PersistentState.NEW;
	}

	public PlayerLunaShop() {
		this.persistentState = PersistentState.NEW;
	}

	/**
	 * @return Whether free chest
	 */
	public boolean isFreeChest() {
		return freeChest;
	}

	/** 设置 free chest / Sets the free chest */
	public void setFreeChest(boolean free) {
		this.freeChest = free;
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置持久化状态。 / Sets the persistent state. */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case UPDATE_REQUIRED:
			if (this.persistentState == PersistentState.NEW) {
				break;
			}
		default:
			this.persistentState = persistentState;
		}
	}
}
