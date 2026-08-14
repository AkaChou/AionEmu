package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 物品冷却时间数据访问对象。
 * Item cooldowns data access object.
 *
 * @author ATracer
 */
public abstract class ItemCooldownsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public final String getClassName() {
		return ItemCooldownsDAO.class.getName();
	}

	/**
	 * 加载玩家的物品冷却时间。
	 * Loads item cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadItemCooldowns(Player player);

	/**
	 * 存储玩家的物品冷却时间。
	 * Stores item cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void storeItemCooldowns(Player player);
}
