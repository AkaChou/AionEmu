package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家冷却时间数据访问对象。
 * Player cooldowns data access object.
 *
 * @author nrg
 */
public abstract class PlayerCooldownsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerCooldownsDAO.class.getName();
	}

	/**
	 * 加载玩家冷却时间数据。
	 * Loads player cooldown data.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadPlayerCooldowns(Player player);

	/**
	 * 存储玩家冷却时间数据。
	 * Stores player cooldown data.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void storePlayerCooldowns(Player player);
}
