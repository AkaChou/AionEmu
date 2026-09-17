package com.aionemu.gameserver.model.gameobjects.player;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerShugoSweepDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 玩家清扫游戏对象。
 * Player Sweep game object.
 */

@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class PlayerSweep {
	/** 获取持久化状态。 / Returns the persistent state. */
	private PersistentState persistentState;

	/** 返回 step / Returns the step */
	private int step;
	/** 返回 free dice / Returns the free dice */
	private int freeDice;
	/** 返回 board id / Returns the board id */
	private int boardId;

	public PlayerSweep(int step, int freeDice, int boardId) {
		this.step = step;
		this.freeDice = freeDice;
		this.boardId = boardId;
		this.persistentState = PersistentState.NEW;
	}

	/** 设置 shugo sweep by obj id / Sets the shugo sweep by obj id */
	public void setShugoSweepByObjId(int playerId) {
		DAOManager.getDAO(PlayerShugoSweepDAO.class).setShugoSweepByObjId(playerId, getFreeDice(), getStep(),
				getBoardId());
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
