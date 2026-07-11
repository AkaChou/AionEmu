package com.aionemu.gameserver.model.skill.linked_skill;

import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * EquippedStigmas 条目，用于技能相关逻辑。
 * Equipped Stigmas Entry for skill logic.
 *
 * @author DrNism
 */
public class EquippedStigmasEntry extends StigmaEntry {

	private PersistentState persistentState;

	public EquippedStigmasEntry(int itemId, String itemName, PersistentState persistentState) {
		super(itemId, itemName);
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
			} else
				this.persistentState = PersistentState.DELETED;
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
