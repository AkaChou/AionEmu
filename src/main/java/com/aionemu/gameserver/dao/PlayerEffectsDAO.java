package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家效果（Buff 等）数据访问对象。
 * Player effects (buffs etc.) data access object.
 *
 * @author ATracer
 */
public abstract class PlayerEffectsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * @return DAO 类名 / DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerEffectsDAO.class.getName();
	}

	/**
	 * 加载玩家效果数据。
	 * Loads player effect data.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadPlayerEffects(Player player);

	/**
	 * 存储玩家效果数据。
	 * Stores player effect data.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void storePlayerEffects(Player player);
}
