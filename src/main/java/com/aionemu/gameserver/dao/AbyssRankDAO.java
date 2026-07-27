package com.aionemu.gameserver.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 欧比斯排名数据访问对象。
 * Abyss rank data access object.
 *
 * @author ATracer
 */
public abstract class AbyssRankDAO implements DAO {

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public final String getClassName() {
		return AbyssRankDAO.class.getName();
	}

	/**
	 * 加载玩家的欧比斯排名数据。
	 * Loads abyss rank data for a player.
	 *
	 * 玩家 / player
	 */
	public abstract void loadAbyssRank(Player player);

	/**
	 * 按玩家 ID 加载欧比斯排名。
	 * Loads abyss rank by player ID.
	 *
	 * player ID
	 * @return 欧比斯排名 / abyss rank
	 */
	public abstract AbyssRank loadAbyssRank(int playerId);

	/**
	 * 存储玩家的欧比斯排名。
	 * Stores a player's abyss rank.
	 *
	 * 玩家 / player
	 * whether successful
	 */
	public abstract boolean storeAbyssRank(Player player);

	public void storeInTransaction(Connection connection, int playerId, AbyssRank rank) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/**
	 * 获取指定种族的玩家欧比斯排行榜。
	 * Gets the player abyss ranking list for a race.
	 *
	 * 阵营 / race
	 * @return 排行结果列表 / ranking result list
	 */
	public abstract ArrayList<AbyssRankingResult> getAbyssRankingPlayers(Race race);

	/**
	 * 获取指定种族的军团欧比斯排行榜。
	 * Gets the legion abyss ranking list for a race.
	 *
	 * 阵营 / race
	 * @return 排行结果列表 / ranking result list
	 */
	public abstract ArrayList<AbyssRankingResult> getAbyssRankingLegions(Race race);

	/**
	 * 加载符合条件的玩家欧比斯点数（AP）。
	 * Loads players' abyss points (AP) matching the given criteria.
	 *
	 * 阵营 / race
	 * lower AP limit
	 * @param maxOfflineDays 最大离线天数 / max offline days
	 * @return 玩家 ID 到 AP 的映射 / map of player ID to AP
	 */
	public abstract Map<Integer, Integer> loadPlayersAp(Race race, final int lowerApLimit, final int maxOfflineDays);

	/**
	 * 加载符合条件的玩家荣耀点数（GP）。
	 * Loads players' glory points (GP) matching the given criteria.
	 *
	 * 阵营 / race
	 * lower GP limit
	 * @param maxOfflineDays 最大离线天数 / max offline days
	 * @return 玩家 ID 到 GP 的映射 / map of player ID to GP
	 */
	public abstract Map<Integer, Integer> loadPlayersGp(Race race, final int lowerGpLimit, final int maxOfflineDays);

	/**
	 * 更新玩家的欧比斯等级。
	 * Updates a player's abyss rank.
	 *
	 * player ID
	 * @param rankEnum 欧比斯等级枚举 / abyss rank enum
	 */
	public abstract void updateAbyssRank(int playerId, AbyssRankEnum rankEnum);

	/**
	 * 刷新欧比斯排行榜。
	 * Updates the abyss rank list.
	 */
	public abstract void updateRankList();

	/**
	 * 从欧比斯排名中移除玩家。
	 * Removes players from the abyss ranking.
	 *
	 * @param listP 玩家列表 / player list
	 */
	public abstract void removePlayer(List<Player> listP);
}
