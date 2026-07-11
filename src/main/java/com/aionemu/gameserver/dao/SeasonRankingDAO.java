package com.aionemu.gameserver.dao;

import java.util.ArrayList;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.Arena6V6Ranking;
import com.aionemu.gameserver.model.gameobjects.player.ranking.ArenaOfTenacityRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.GoldArenaRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.TowerOfChallengeRank;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;

/**
 * 赛季排名数据访问抽象层。
 * DAO for season ranking persistence across arena modes.
 *
 * Created by Wnkrz on 24/07/2017.
 */
public abstract class SeasonRankingDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return SeasonRankingDAO.class.getName();
	}

	/**
	 * 查询指定表的竞赛排名玩家列表。
	 * Returns competition ranking players for the given table.
	 *
	 * ranking table id
	 * @return 赛季排名结果列表 / season ranking result list
	 */
	public abstract ArrayList<SeasonRankingResult> getCompetitionRankingPlayers(int tableId);

	/**
	 * 加载玩家黄金竞技场排名。
	 * Loads Gold Arena rank for the player.
	 *
	 * player object id
	 * ranking table id
	 * @return 黄金竞技场排名 / gold arena rank
	 */
	public abstract GoldArenaRank loadGoldArenaRank(int playerId, int tableId);

	/**
	 * 加载玩家孤独竞技场排名。
	 * Loads Arena of Tenacity rank for the player.
	 *
	 * player object id
	 * ranking table id
	 * @return 孤独竞技场排名 / arena of tenacity rank
	 */
	public abstract ArenaOfTenacityRank loadArenaOfTenacityRank(int playerId, int tableId);

	/**
	 * 加载玩家挑战之塔排名。
	 * Loads Tower of Challenge rank for the player.
	 *
	 * player object id
	 * ranking table id
	 * @return 挑战之塔排名 / tower of challenge rank
	 */
	public abstract TowerOfChallengeRank loadTowerOfChallengeRank(int playerId, int tableId);

	/**
	 * 加载玩家 6v6 竞技场排名。
	 * Loads Arena 6v6 rank for the player.
	 *
	 * player object id
	 * ranking table id
	 * arena 6v6 ranking
	 */
	public abstract Arena6V6Ranking loadArena6v6Rank(int playerId, int tableId);

	/**
	 * 保存玩家黄金竞技场排名。
	 * Stores Gold Arena rank for the player.
	 *
	 * 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeGoldArenaRank(Player player);

	/**
	 * 保存玩家挑战之塔排名。
	 * Stores Tower of Challenge rank for the player.
	 *
	 * 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeTowerRank(Player player);

	/**
	 * 保存玩家孤独竞技场排名。
	 * Stores Arena of Tenacity rank for the player.
	 *
	 * 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean storeTenacityRank(Player player);

	/**
	 * 保存玩家 6v6 竞技场排名。
	 * Stores Arena 6v6 rank for the player.
	 *
	 * 玩家 / player
	 * @return 是否保存成功 / true if stored
	 */
	public abstract boolean store6v6Rank(Player player);
}
