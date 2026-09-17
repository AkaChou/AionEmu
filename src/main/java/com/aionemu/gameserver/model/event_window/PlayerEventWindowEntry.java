package com.aionemu.gameserver.model.event_window;

import java.sql.Timestamp;

import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;

/**
 * 玩家活动窗口条目，用于活动窗口相关逻辑。
 * Player Event Window Entry for event window logic.
 *
 * @author Ranastic
 */
@Getter
public class PlayerEventWindowEntry extends EventWindowEntry {

	/** 获取持久化状态。 / Returns the persistent state. */
	private PersistentState persistentState;

	public PlayerEventWindowEntry(int id, Timestamp lastStamp, int elapsed, PersistentState persistentState) {
		super(id, lastStamp, elapsed);
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
