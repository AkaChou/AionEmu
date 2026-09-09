package com.aionemu.gameserver.model.gameobjects.player;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * 玩家月华 Shop 游戏对象。
 * Player Luna Shop game object.
 */

@Slf4j
@NoArgsConstructor
public class PlayerLunaShop {
	/** 获取持久化状态。 / Returns the persistent state. */
	@Getter
	private PersistentState persistentState;

	/**
	 * @return 是否免费开启地下通道 / Whether free underpath
	 */
	@Getter
	@Setter
	private boolean FreeUnderpath;
	/**
	 * @return 是否免费开启工坊 / Whether free factory
	 */
	@Getter
	@Setter
	private boolean FreeFactory;
	/**
	 * @return 是否免费开启宝箱 / Whether free chest
	 */
	@Getter
	@Setter
	private boolean FreeChest;

	public PlayerLunaShop(boolean freeUnderpath, boolean freeFactory, boolean freeChest) {
		this.FreeUnderpath = freeUnderpath;
		this.FreeFactory = freeFactory;
		this.FreeChest = freeChest;
		this.persistentState = PersistentState.NEW;
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
