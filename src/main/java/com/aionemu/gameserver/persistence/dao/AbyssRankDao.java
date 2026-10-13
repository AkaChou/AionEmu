package com.aionemu.gameserver.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.AbyssRankPo;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.mybatisflex.core.service.IService;

/**
 * 欧比斯排名数据访问对象。
 * Abyss rank data access object.
 *
 * @author ATracer
 */
public interface AbyssRankDao extends IService<AbyssRankPo> {

	/**
	 * 加载玩家的欧比斯排名数据。
	 * Loads abyss rank data for a player.
	 *
	 * @param player 玩家 / player
	 */
	void loadAbyssRank(Player player);

	/**
	 * 按玩家 ID 加载欧比斯排名。
	 * Loads abyss rank by player ID.
	 *
	 * @param playerId 玩家 ID / player ID
	 * @return 欧比斯排名 / abyss rank
	 */
	AbyssRank loadAbyssRank(int playerId);

	/**
	 * 存储玩家的欧比斯排名。
	 * Stores a player's abyss rank.
	 *
	 * @param player 玩家 / player
	 */
	void storeAbyssRank(Player player);

	/**
	 * 在调用方事务内存储欧比斯排名。
	 * Stores the abyss rank inside the caller's transaction.
	 *
	 * @param playerId 玩家 ID / player ID
	 * @param rank     欧比斯排名 / abyss rank
	 */
	void storeInTransaction(int playerId, AbyssRank rank);

	/**
	 * 获取指定种族的玩家欧比斯排行榜。
	 * Gets the player abyss ranking list for a race.
	 *
	 * @param race 阵营 / race
	 * @return 排行结果列表 / ranking result list
	 */
	ArrayList<AbyssRankingResult> getAbyssRankingPlayers(Race race);

	/**
	 * 获取指定种族的军团欧比斯排行榜。
	 * Gets the legion abyss ranking list for a race.
	 *
	 * @param race 阵营 / race
	 * @return 排行结果列表 / ranking result list
	 */
	ArrayList<AbyssRankingResult> getAbyssRankingLegions(Race race);

	/**
	 * 加载符合条件的玩家荣耀点数（GP）。
	 * Loads players' glory points (GP) matching the given criteria.
	 *
	 * @param race           阵营 / race
	 * @param lowerGpLimit   最低 GP 下限 / lower GP limit
	 * @param maxOfflineDays 最大离线天数 / max offline days
	 * @return 玩家 ID 到 GP 的映射 / map of player ID to GP
	 */
	Map<Integer, Integer> loadPlayersGp(Race race, int lowerGpLimit, int maxOfflineDays);

	/**
	 * 更新玩家的欧比斯等级。
	 * Updates a player's abyss rank.
	 *
	 * @param playerId 玩家 ID / player ID
	 * @param rankEnum 欧比斯等级枚举 / abyss rank enum
	 */
	void updateAbyssRank(int playerId, AbyssRankEnum rankEnum);

	/**
	 * 刷新欧比斯排行榜。
	 * Updates the abyss rank list.
	 */
	void updateRankList();

	/**
	 * 从欧比斯排名中移除玩家。
	 * Removes players from the abyss ranking.
	 *
	 * @param listP 玩家列表 / player list
	 */
	void removePlayer(List<Player> listP);
}
