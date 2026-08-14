package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 房屋物件冷却时间数据访问对象。
 * House object cooldowns data access object.
 */
public abstract class HouseObjectCooldownsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return HouseObjectCooldownsDAO.class.getName();
	}

	/**
	 * 加载玩家的房屋物件冷却时间。
	 * Loads house object cooldowns for a player.
	 *
	 * @param paramPlayer 玩家 / player
	 */
	public abstract void loadHouseObjectCooldowns(Player paramPlayer);

	/**
	 * 存储玩家的房屋物件冷却时间。
	 * Stores house object cooldowns for a player.
	 *
	 * @param paramPlayer 玩家 / player
	 */
	public abstract void storeHouseObjectCooldowns(Player paramPlayer);
}
