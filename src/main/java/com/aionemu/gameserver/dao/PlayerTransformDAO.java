package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家变身面板数据访问抽象层。
 * DAO for player transformation panel persistence.
 */
public abstract class PlayerTransformDAO implements DAO {

	/**
	 * 加载玩家变身数据。
	 * Loads transformation data for the player.
	 *
	 * 玩家 / player
	 */
	public abstract void loadPlTransfo(Player player);

	/**
	 * 保存玩家变身面板数据。
	 * Stores transformation panel data for the player.
	 *
	 * player object id
	 * panel id
	 * item id
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storePlTransfo(int playerId, int panelId, int ItemId);

	/**
	 * 删除玩家变身数据。
	 * Deletes transformation data for the player.
	 *
	 * player object id
	 * @return 是否删除成功 / true if deleted
	 */
	public abstract boolean deletePlTransfo(int playerId);

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	public String getClassName() {
		return PlayerTransformDAO.class.getName();
	}
}
