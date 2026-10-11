package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.*;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 赛季竞技排行数据访问对象的 MySQL 8 实现。
 * 覆盖黄金竞技场、挑战之塔、孤独竞技场、6v6 等 competition_ranking 表数据。
 * MySQL 8 implementation of SeasonRankingDAO using JdbcClient.
 * @author Wnkrz
 */
@Slf4j
@Repository
public class SeasonRankingDAO extends com.aionemu.gameserver.dao.SeasonRankingDAO {

    /** 查询排行榜前 300 名玩家 / Select top 300 competition ranking players */
    public static final String SELECT_PLAYERS_RANKING = "SELECT competition_ranking.rank, competition_ranking.last_rank, " + "competition_ranking.points, competition_ranking.player_id, players.name, " + "players.id, players.player_class, players.race FROM competition_ranking " + "INNER JOIN players ON competition_ranking.player_id = players.id " + "WHERE competition_ranking.table_id = ? AND competition_ranking.points > 0 " + "ORDER BY competition_ranking.points DESC LIMIT 300";

    /** 查询玩家某表排行记录 / Select a player's ranking row by table id */
    public static final String SELECT_MY_HISTORY = "SELECT * FROM competition_ranking WHERE player_id = ? AND table_id = ?";

    /** 插入排行记录 / Insert a competition ranking row */
    public static final String INSERT_QUERY = "INSERT INTO competition_ranking (player_id, table_id, rank, last_rank, " + "points, last_points, high_points, low_points, position_match) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 更新排行记录 / Update a competition ranking row */
    public static final String UPDATE_QUERY = "UPDATE competition_ranking SET rank = ?, last_rank = ?, points = ?, " + "last_points = ?, high_points = ?, low_points = ?, position_match = ? " + "WHERE player_id = ? AND table_id = ?";

    private final JdbcClient jdbcClient;

    public SeasonRankingDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public SeasonRankingDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 获取指定排行表的前 300 名竞技排行玩家。
     * Loads the top 300 competition ranking players for the given table id.
     * @param tableId 排名表 ID / ranking table id
     * @return 排行结果列表 / ranking result list
     */
    @Override
    public ArrayList<SeasonRankingResult> getCompetitionRankingPlayers(int tableId) {
        ArrayList<SeasonRankingResult> results = new ArrayList<>();

        try {
            jdbcClient.sql(SELECT_PLAYERS_RANKING)
                .param(tableId)
                .query((resultSet, _) -> {
                    String name = resultSet.getString("players.name");
                    int rank = resultSet.getInt("competition_ranking.rank");
                    int last_rank = resultSet.getInt("competition_ranking.last_rank");
                    int points = resultSet.getInt("competition_ranking.points");
                    int playerId = resultSet.getInt("players.id");
                    String playerClassStr = resultSet.getString("players.player_class");

                    PlayerClass playerClass;
                    try {
                        playerClass = PlayerClass.getPlayerClassByString(playerClassStr);
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.9e67b184f360", playerClassStr));
                        return null;
                    }

                    String raceStr = resultSet.getString("players.race");
                    Race race = Race.getRaceByString(raceStr);

                    if (playerClass != null && race != null) {
                        results.add(new SeasonRankingResult(name, last_rank, rank, points, playerClass, race.getRaceId(), playerId));
                    }
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2d8dc2963806", tableId), e);
        }

        return results;
    }

    /**
     * 加载玩家黄金竞技场排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's Gold Arena rank; returns a NEW empty rank when none exists.
     * @param playerId 玩家对象 ID / player object id
     * @param tableId 排名表 ID / ranking table id
     * @return 黄金竞技场排行 / Gold Arena rank
     */
    @Override
    public GoldArenaRank loadGoldArenaRank(int playerId, int tableId) {
        try {
            GoldArenaRank arenaRank = jdbcClient.sql(SELECT_MY_HISTORY)
                .param(playerId)
                .param(tableId)
                .query((resultSet, _) -> {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");
                    int position_match = resultSet.getInt("position_match");

                    GoldArenaRank r = new GoldArenaRank(rank, best_rank, point, last_point, high_point, low_point, position_match);
                    r.setPersistentState(PersistentState.UPDATED);
                    return r;
                })
                .optional()
                .orElse(null);

            if (arenaRank == null) {
                arenaRank = new GoldArenaRank(0, 0, 0, 0, 0, 0, 0);
                arenaRank.setPersistentState(PersistentState.NEW);
            }
            return arenaRank;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.d516d865c7d9", playerId, tableId), e);
            GoldArenaRank fallback = new GoldArenaRank(0, 0, 0, 0, 0, 0, 0);
            fallback.setPersistentState(PersistentState.NEW);
            return fallback;
        }
    }

    

    private boolean addGoldRank(final int objectId, final GoldArenaRank rank) {
        try {
            int affected = jdbcClient.sql(INSERT_QUERY)
                .param(objectId)
                .param(1)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getPoints())
                .param(rank.getLastPoints())
                .param(rank.getHighPoints())
                .param(rank.getLowPoints())
                .param(rank.getPossitionMatch())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.892315d315f0", objectId), e);
            return false;
        }
    }

    private boolean updateGoldRank(final int objectId, GoldArenaRank rank) {
        try {
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getPoints())
                .param(rank.getLastPoints())
                .param(rank.getHighPoints())
                .param(rank.getLowPoints())
                .param(rank.getPossitionMatch())
                .param(objectId)
                .param(1)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8025a55d13a2", objectId), e);
            return false;
        }
    }

    /**
     * 按持久化状态保存玩家挑战之塔排行（table_id = 2）。
     * Stores the player's Tower of Challenge rank by persistent state (table_id = 2).
     * @param player 玩家 / player
     * @return 是否保存成功；无排行对象时返回 false / whether store succeeded; false if rank is null
     */
    @Override
    public void storeTowerRank(Player player) {
        TowerOfChallengeRank rank = player.getTowerRank();
        if (rank == null) {
            return;
        }

        boolean result ;
        switch (rank.getPersistentState()) {
            case NEW:
                result = addTowerRank(player.getObjectId(), rank);
                break;
            case UPDATE_REQUIRED:
                result = updateTowerRank(player.getObjectId(), rank);
                break;
            default:
                return;
        }

        if (result) {
            rank.setPersistentState(PersistentState.UPDATED);
        }
    }

    private boolean addTowerRank(final int objectId, final TowerOfChallengeRank rank) {
        try {
            int affected = jdbcClient.sql(INSERT_QUERY)
                .param(objectId)
                .param(2)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getCurrentTime())
                .param(rank.getLastTime())
                .param(rank.getBestTime())
                .param(rank.getLowRank())
                .param(0)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.33e0d184fd35", objectId), e);
            return false;
        }
    }

    private boolean updateTowerRank(final int objectId, TowerOfChallengeRank rank) {
        try {
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getCurrentTime())
                .param(rank.getLastTime())
                .param(rank.getBestTime())
                .param(rank.getLowRank())
                .param(0)
                .param(objectId)
                .param(2)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.525c0a41a0d5", objectId), e);
            return false;
        }
    }

    

    private boolean addTenacityRank(final int objectId, final ArenaOfTenacityRank rank) {
        try {
            int affected = jdbcClient.sql(INSERT_QUERY)
                .param(objectId)
                .param(541)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getPoints())
                .param(rank.getLastPoints())
                .param(rank.getHighPoints())
                .param(rank.getLowPoints())
                .param(rank.getPossitionMatch())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.1833d5239851", objectId), e);
            return false;
        }
    }

    private boolean updateTenacityRank(final int objectId, ArenaOfTenacityRank rank) {
        try {
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getPoints())
                .param(rank.getLastPoints())
                .param(rank.getHighPoints())
                .param(rank.getLowPoints())
                .param(rank.getPossitionMatch())
                .param(objectId)
                .param(541)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.b547e9af94b6", objectId), e);
            return false;
        }
    }

    

    private boolean add6v6Rank(final int objectId, final Arena6V6Ranking rank) {
        try {
            int affected = jdbcClient.sql(INSERT_QUERY)
                .param(objectId)
                .param(3)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getPoints())
                .param(rank.getLastPoints())
                .param(rank.getHighPoints())
                .param(rank.getLowPoints())
                .param(rank.getPossitionMatch())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.688e68aa0f83", objectId), e);
            return false;
        }
    }

    private boolean update6v6Rank(final int objectId, Arena6V6Ranking rank) {
        try {
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(rank.getRank())
                .param(rank.getBestRank())
                .param(rank.getPoints())
                .param(rank.getLastPoints())
                .param(rank.getHighPoints())
                .param(rank.getLowPoints())
                .param(rank.getPossitionMatch())
                .param(objectId)
                .param(3)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2027d7f241eb", objectId), e);
            return false;
        }
    }

    /**
     * 加载玩家孤独竞技场排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's Arena of Tenacity rank; returns a NEW empty rank when none exists.
     * @param playerId 玩家对象 ID / player object id
     * @param tableId 排名表 ID / ranking table id
     * @return 孤独竞技场排行 / Arena of Tenacity rank
     */
    @Override
    public ArenaOfTenacityRank loadArenaOfTenacityRank(int playerId, int tableId) {
        try {
            ArenaOfTenacityRank ranking = jdbcClient.sql(SELECT_MY_HISTORY)
                .param(playerId)
                .param(tableId)
                .query((resultSet, _) -> {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");
                    int position_match = resultSet.getInt("position_match");

                    ArenaOfTenacityRank r = new ArenaOfTenacityRank(rank, best_rank, point, last_point, high_point, low_point, position_match);
                    r.setPersistentState(PersistentState.UPDATED);
                    return r;
                })
                .optional()
                .orElse(null);

            if (ranking == null) {
                ranking = new ArenaOfTenacityRank(0, 0, 0, 0, 0, 0, 0);
                ranking.setPersistentState(PersistentState.NEW);
            }
            return ranking;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.1c6ce1dd5b7e", playerId, tableId), e);
            ArenaOfTenacityRank fallback = new ArenaOfTenacityRank(0, 0, 0, 0, 0, 0, 0);
            fallback.setPersistentState(PersistentState.NEW);
            return fallback;
        }
    }

    /**
     * 加载玩家挑战之塔排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's Tower of Challenge rank; returns a NEW empty rank when none exists.
     * @param playerId 玩家对象 ID / player object id
     * @param tableId 排名表 ID / ranking table id
     * @return 挑战之塔排行 / Tower of Challenge rank
     */
    @Override
    public TowerOfChallengeRank loadTowerOfChallengeRank(int playerId, int tableId) {
        try {
            TowerOfChallengeRank ranking = jdbcClient.sql(SELECT_MY_HISTORY)
                .param(playerId)
                .param(tableId)
                .query((resultSet, _) -> {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");

                    TowerOfChallengeRank r = new TowerOfChallengeRank(rank, best_rank, point, last_point, high_point, low_point);
                    r.setPersistentState(PersistentState.UPDATED);
                    return r;
                })
                .optional()
                .orElse(null);

            if (ranking == null) {
                ranking = new TowerOfChallengeRank(0, 0, 0, 0, 0, 0);
                ranking.setPersistentState(PersistentState.NEW);
            }
            return ranking;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.a937504b345f", playerId, tableId), e);
            TowerOfChallengeRank fallback = new TowerOfChallengeRank(0, 0, 0, 0, 0, 0);
            fallback.setPersistentState(PersistentState.NEW);
            return fallback;
        }
    }

    /**
     * 加载玩家 6v6 竞技场排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's 6v6 Arena rank; returns a NEW empty rank when none exists.
     * @param playerId 玩家对象 ID / player object id
     * @param tableId 排名表 ID / ranking table id
     * @return 6v6 竞技场排行 / 6v6 Arena rank
     */
    @Override
    public Arena6V6Ranking loadArena6v6Rank(int playerId, int tableId) {
        try {
            Arena6V6Ranking ranking = jdbcClient.sql(SELECT_MY_HISTORY)
                .param(playerId)
                .param(tableId)
                .query((resultSet, _) -> {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");
                    int position_match = resultSet.getInt("position_match");

                    Arena6V6Ranking r = new Arena6V6Ranking(rank, best_rank, point, last_point, high_point, low_point, position_match);
                    r.setPersistentState(PersistentState.UPDATED);
                    return r;
                })
                .optional()
                .orElse(null);

            if (ranking == null) {
                ranking = new Arena6V6Ranking(0, 0, 0, 0, 0, 0, 0);
                ranking.setPersistentState(PersistentState.NEW);
            }
            return ranking;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.a8b4326794d1", playerId, tableId), e);
            Arena6V6Ranking fallback = new Arena6V6Ranking(0, 0, 0, 0, 0, 0, 0);
            fallback.setPersistentState(PersistentState.NEW);
            return fallback;
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持（MySQL 8）。
     * Checks whether the given database is supported by this DAO (MySQL 8).
     * @param databaseName 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
