package com.aionemu.gameserver.model.gameobjects.player;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerShugoSweepDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 玩家清扫游戏对象。
 * Player Sweep game object.
 */

@Slf4j
public class PlayerSweep {
	private PersistentState persistentState;

	private int step;
	private int freeDice;
	private int boardId;

	public PlayerSweep(int step, int freeDice, int boardId) {
		this.step = step;
		this.freeDice = freeDice;
		this.boardId = boardId;
		this.persistentState = PersistentState.NEW;
	}

	public PlayerSweep() {
	}

	/** 返回 free dice / Returns the free dice */
	public int getFreeDice() {
		return freeDice;
	}

	/** 设置 free dice / Sets the free dice */
	public void setFreeDice(int dice) {
		this.freeDice = dice;
	}

	/** 返回 step / Returns the step */
	public int getStep() {
		return step;
	}

	/** 设置 step / Sets the step */
	public void setStep(int step) {
		this.step = step;
	}

	/** 返回 board id / Returns the board id */
	public int getBoardId() {
		return boardId;
	}

	/** 设置 board id / Sets the board id */
	public void setBoardId(int boardId) {
		this.boardId = boardId;
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
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
