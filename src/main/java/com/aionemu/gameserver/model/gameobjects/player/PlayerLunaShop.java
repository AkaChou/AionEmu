package com.aionemu.gameserver.model.gameobjects.player;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * 玩家月华 Shop 游戏对象。
 * Player Luna Shop game object.
 */

@Slf4j
public class PlayerLunaShop {
	private PersistentState persistentState;

	private boolean FreeUnderpath;
	private boolean FreeFactory;
	private boolean FreeChest;

	public PlayerLunaShop(boolean freeUnderpath, boolean freeFactory, boolean freeChest) {
		this.FreeUnderpath = freeUnderpath;
		this.FreeFactory = freeFactory;
		this.FreeChest = freeChest;
		this.persistentState = PersistentState.NEW;
	}

	public PlayerLunaShop() {
	}

	/**
	 * @return Whether free underpath
	 */
	public boolean isFreeUnderpath() {
		return FreeUnderpath;
	}

	/** 设置 free underpath / Sets the free underpath */
	public void setFreeUnderpath(boolean free) {
		this.FreeUnderpath = free;
	}

	/**
	 * @return Whether free factory
	 */
	public boolean isFreeFactory() {
		return FreeFactory;
	}

	/** 设置 free factory / Sets the free factory */
	public void setFreeFactory(boolean free) {
		this.FreeFactory = free;
	}

	/**
	 * @return Whether free chest
	 */
	public boolean isFreeChest() {
		return FreeChest;
	}

	/** 设置 free chest / Sets the free chest */
	public void setFreeChest(boolean free) {
		this.FreeChest = free;
	}

	/** 获取持久化状态。 / Returns the persistent state. */
	public PersistentState getPersistentState() {
		return persistentState;
	}

	/** 设置 luna shop by obj id / Sets the luna shop by obj id */
	public void setLunaShopByObjId(int playerId) {
		DAOManager.getDAO(PlayerLunaShopDAO.class).setLunaShopByObjId(playerId, isFreeUnderpath(), isFreeFactory(),
				isFreeChest());
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
