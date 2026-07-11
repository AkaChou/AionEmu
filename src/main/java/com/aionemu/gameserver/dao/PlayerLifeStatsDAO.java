package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家生命状态（HP/MP 等）数据访问对象。
 * Player life-stats data access object.
 *
 * @author Mr. Poke
 */
public abstract class PlayerLifeStatsDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier of this DAO.
	 *
	 * DAO class name
	 */
	@Override
	public final String getClassName() {
		return PlayerLifeStatsDAO.class.getName();
	}

	/**
	 * 加载玩家生命状态。
	 * Loads player life stats.
	 *
	 * 玩家 / player
	 */
	public abstract void loadPlayerLifeStat(Player player);

	/**
	 * 插入玩家生命状态记录。
	 * Inserts a player life-stat record.
	 *
	 * 玩家 / player
	 */
	public abstract void insertPlayerLifeStat(Player player);

	/**
	 * 更新玩家生命状态记录。
	 * Updates a player life-stat record.
	 *
	 * 玩家 / player
	 */
	public abstract void updatePlayerLifeStat(Player player);
}
