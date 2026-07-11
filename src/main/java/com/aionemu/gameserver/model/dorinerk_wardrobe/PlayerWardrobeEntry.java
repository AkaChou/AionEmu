package com.aionemu.gameserver.model.dorinerk_wardrobe;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;

/**
 * 玩家衣橱条目，用于多里纳克衣橱相关逻辑。
 * Player Wardrobe Entry for dorinerk wardrobe logic.
 *
 * @author Ranastic
 */
public class PlayerWardrobeEntry extends WardrobeEntry {

	@Getter
	private PersistentState persistentState;

	public PlayerWardrobeEntry(int itemId, int slot, int reskin_count, PersistentState persistentState) {
		super(itemId, slot, reskin_count);
		this.persistentState = persistentState;
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
