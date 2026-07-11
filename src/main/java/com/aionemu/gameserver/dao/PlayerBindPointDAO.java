package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家绑定点（回城点）数据访问对象。
 * Player bind-point data access object.
 *
 * @author evilset
 */
public abstract class PlayerBindPointDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public String getClassName() {
		return PlayerBindPointDAO.class.getName();
	}

	/**
	 * 加载玩家绑定点。
	 * Loads the player's bind point.
	 *
	 * 玩家 / player
	 */
	public abstract void loadBindPoint(Player player);

	/**
	 * 插入玩家绑定点记录。
	 * Inserts a player bind-point record.
	 *
	 * 玩家 / player
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean insertBindPoint(Player player);

	/**
	 * 更新玩家绑定点记录。
	 * Updates a player bind-point record.
	 *
	 * 玩家 / player
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean updateBindPoint(Player player);

	/**
	 * 持久化玩家绑定点（插入或更新）。
	 * Persists the player's bind point (insert or update).
	 *
	 * 玩家 / player
	 * 若 successful 则为 true / true if successful
	 */
	public abstract boolean store(Player player);
}
