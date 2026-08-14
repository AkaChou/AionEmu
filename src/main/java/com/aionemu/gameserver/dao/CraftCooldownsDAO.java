package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 制作冷却时间数据访问对象。
 * Craft cooldowns data access object.
 *
 * @author synchro2
 */
public abstract class CraftCooldownsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * @return 类名 / class name
	 */
	@Override
	public final String getClassName() {
		return CraftCooldownsDAO.class.getName();
	}

	/**
	 * 加载玩家的制作冷却时间。
	 * Loads craft cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadCraftCooldowns(Player player);

	/**
	 * 存储玩家的制作冷却时间。
	 * Stores craft cooldowns for a player.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void storeCraftCooldowns(Player player);

}
