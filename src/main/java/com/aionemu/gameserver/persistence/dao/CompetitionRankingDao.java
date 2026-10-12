package com.aionemu.gameserver.persistence.dao;

import java.util.ArrayList;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.Arena6V6Ranking;
import com.aionemu.gameserver.model.gameobjects.player.ranking.ArenaOfTenacityRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.GoldArenaRank;
import com.aionemu.gameserver.model.gameobjects.player.ranking.TowerOfChallengeRank;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;
import com.aionemu.gameserver.persistence.entity.CompetitionRankingPo;
import com.mybatisflex.core.service.IService;

/**
 * 赛季排名数据访问抽象层。
 * DAO for season ranking persistence across arena modes.
 * Created by Wnkrz on 24/07/2017.
 */
public interface CompetitionRankingDao extends IService<CompetitionRankingPo> {

	/**
	 * 查询指定表的竞赛排名玩家列表。
	 * Returns competition ranking players for the given table.
	 *
	 * @param tableId 排行表 ID / ranking table id
	 * @return 赛季排名结果列表 / season ranking result list
	 */
	ArrayList<SeasonRankingResult> getCompetitionRankingPlayers(int tableId);

	/**
	 * 加载玩家黄金竞技场排名。
	 * Loads Gold Arena rank for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId  排行表 ID / ranking table id
	 * @return 黄金竞技场排名 / gold arena rank
	 */
	GoldArenaRank loadGoldArenaRank(int playerId, int tableId);

	/**
	 * 加载玩家孤独竞技场排名。
	 * Loads Arena of Tenacity rank for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId  排行表 ID / ranking table id
	 * @return 孤独竞技场排名 / arena of tenacity rank
	 */
	ArenaOfTenacityRank loadArenaOfTenacityRank(int playerId, int tableId);

	/**
	 * 加载玩家挑战之塔排名。
	 * Loads Tower of Challenge rank for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId  排行表 ID / ranking table id
	 * @return 挑战之塔排名 / tower of challenge rank
	 */
	TowerOfChallengeRank loadTowerOfChallengeRank(int playerId, int tableId);

	/**
	 * 加载玩家 6v6 竞技场排名。
	 * Loads Arena 6v6 rank for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 * @param tableId  排行表 ID / ranking table id
	 * @return 6v6 竞技场排名 / arena 6v6 ranking
	 */
	Arena6V6Ranking loadArena6v6Rank(int playerId, int tableId);

	/**
	 * 保存玩家挑战之塔排名。
	 * Stores Tower of Challenge rank for the player.
	 *
	 * @param player 玩家 / player
	 */
	void storeTowerRank(Player player);
}
