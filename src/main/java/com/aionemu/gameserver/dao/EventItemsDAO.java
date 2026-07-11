package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 活动物品数据访问对象。
 * Event items data access object.
 *
 * @author wanke
 */
public abstract class EventItemsDAO implements DAO {
	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public final String getClassName() {
		return EventItemsDAO.class.getName();
	}

	/**
	 * 加载玩家的活动物品。
	 * Loads event items for a player.
	 *
	 * 玩家 / player
	 */
	public abstract void loadItems(Player player);

	/**
	 * 存储玩家的活动物品。
	 * Stores event items for a player.
	 *
	 * 玩家 / player
	 */
	public abstract void storeItems(Player player);

	/**
	 * 删除指定模板 ID 的活动物品记录。
	 * Deletes event item records for the given item template ID.
	 *
	 * item template ID
	 */
	public abstract void deleteItems(final int itemId);
}
