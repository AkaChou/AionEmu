package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 欧比斯军阶数据访问对象的 MySQL 8 实现，已修复连接泄漏。
 * MySQL 8 implementation of AbyssRankDAO with connection leak fixes.
 */
@Slf4j
public class AbyssRankDAO extends com.aionemu.gameserver.dao.AbyssRankDAO {


    /** 按玩家 ID 查询欧比斯军阶 / Select abyss rank by player id */
    public static final String SELECT_QUERY = "SELECT daily_ap, daily_gp, weekly_ap, weekly_gp, ap, gp, `rank`, " + "top_ranking, daily_kill, weekly_kill, all_kill, max_rank, last_kill, " + "last_ap, last_gp, last_update FROM abyss_rank WHERE player_id = ?";

    /** 插入欧比斯军阶记录 / Insert abyss rank row */
    public static final String INSERT_QUERY = "INSERT INTO abyss_rank (player_id, daily_ap, daily_gp, weekly_ap, " + "weekly_gp, ap, gp, `rank`, top_ranking, daily_kill, weekly_kill, " + "all_kill, max_rank, last_kill, last_ap, last_gp, last_update) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 更新欧比斯军阶记录 / Update abyss rank row */
    public static final String UPDATE_QUERY = "UPDATE abyss_rank SET daily_ap = ?, daily_gp = ?, weekly_ap = ?, " + "weekly_gp = ?, ap = ?, gp = ?, `rank` = ?, top_ranking = ?, daily_kill = ?, " +  "weekly_kill = ?, all_kill = ?, max_rank = ?, last_kill = ?, last_ap = ?, " + "last_gp = ?, last_update = ? WHERE player_id = ?";

    /** 查询阵营玩家欧比斯排行 / Select player abyss ranking for a race */
    public static final String SELECT_PLAYERS_RANKING = "SELECT abyss_rank.`rank`, abyss_rank.ap, abyss_rank.gp, abyss_rank.old_rank_pos, " + "abyss_rank.rank_pos, players.name, legions.name, players.id, players.title_id, " + "players.player_class, players.exp FROM abyss_rank INNER JOIN players " + "ON abyss_rank.player_id = players.id LEFT JOIN legion_members " + "ON legion_members.player_id = players.id LEFT JOIN legions " + "ON legions.id = legion_members.legion_id WHERE players.race = ? " + "AND abyss_rank.gp > 1243 ORDER BY abyss_rank.gp DESC LIMIT 300";

    /** 查询阵营军团欧比斯排行 / Select legion abyss ranking for a race */
    public static final String SELECT_LEGIONS_RANKING = "SELECT legions.id, legions.name, legions.contribution_points, " + "legions.level as lvl, legions.old_rank_pos, legions.rank_pos " + "FROM legions, legion_members, players WHERE players.race = ? " + "AND legion_members.`rank` = 'BRIGADE_GENERAL' AND legion_members.player_id = players.id " + "AND legion_members.legion_id = legions.id AND legions.contribution_points > 0 " + "GROUP BY id ORDER BY legions.contribution_points DESC LIMIT 50";

    /** 按阵营查询玩家 AP / Select player AP by race */
    public static final String SELECT_AP_PLAYER = "SELECT player_id, ap FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND ap > ? " + "ORDER BY ap DESC";

    /** 按阵营查询活跃玩家 AP / Select active player AP by race */
    public static final String SELECT_AP_PLAYER_ACTIVE_ONLY =  "SELECT player_id, ap FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND ap > ? " + "AND UNIX_TIMESTAMP(CURDATE()) - UNIX_TIMESTAMP(players.last_online) <= ? * 24 * 60 * 60 " + "ORDER BY ap DESC";

    /** 按阵营查询玩家 GP / Select player GP by race */
    public static final String SELECT_GP_PLAYER = "SELECT player_id, gp FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND gp > ? " + "ORDER BY gp DESC";

    /** 按阵营查询活跃玩家 GP / Select active player GP by race */
    public static final String SELECT_GP_PLAYER_ACTIVE_ONLY =  "SELECT player_id, gp FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND gp > ? " + "AND UNIX_TIMESTAMP(CURDATE()) - UNIX_TIMESTAMP(players.last_online) <= ? * 24 * 60 * 60 " + "ORDER BY gp DESC";

    /** 更新玩家军阶与名额 / Update player rank and top ranking quota */
    public static final String UPDATE_RANK = "UPDATE abyss_rank SET `rank` = ?, top_ranking = ? WHERE player_id = ?";

    /** 统计军团成员数 / Count legion members */
    public static final String SELECT_LEGION_COUNT = "SELECT COUNT(player_id) as players FROM legion_members WHERE legion_id = ?";

    /** 按 GP 重算玩家排行位置 / Recalculate player rank positions by GP */
    public static final String UPDATE_PLAYER_RANK_LIST = "UPDATE abyss_rank SET abyss_rank.old_rank_pos = abyss_rank.rank_pos, " + "abyss_rank.rank_pos = @a:=@a+1 WHERE player_id IN " + "(SELECT id FROM players WHERE race = ?) ORDER BY gp DESC ";

    /** 按贡献点重算军团排行位置 / Recalculate legion rank positions by contribution */
    public static final String UPDATE_LEGION_RANK_LIST = "UPDATE legions SET legions.old_rank_pos = legions.rank_pos, " + "legions.rank_pos = @a:=@a+1 WHERE id IN " + "(SELECT legion_id FROM legion_members, players WHERE legion_members.`rank` = 'BRIGADE_GENERAL' " + "AND players.id = legion_members.player_id AND players.race = ?) " + "ORDER BY legions.contribution_points DESC ";

    private static String updatePlayerRankList() {
        return UPDATE_PLAYER_RANK_LIST + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 500" : "");
    }

    private static String updateLegionRankList() {
        return UPDATE_LEGION_RANK_LIST + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 75" : "");
    }

    /** 删除玩家欧比斯军阶记录 / Delete player abyss rank row */
    public static final String DELETE_QUERY = "DELETE FROM `abyss_rank` WHERE player_id = ?";

    /**
     * 按玩家 ID 加载欧比斯军阶；无记录时创建默认 NEW 状态实例。
     * Loads abyss rank by player id; creates a default NEW instance when missing.
     *
     * player id
     *
     * @param playerId
     * @return 欧比斯军阶 / abyss rank
     */
    @Override
    public AbyssRank loadAbyssRank(int playerId) {
        AbyssRank abyssRank = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, playerId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int daily_ap = resultSet.getInt("daily_ap");
                    int daily_gp = resultSet.getInt("daily_gp");
                    int weekly_ap = resultSet.getInt("weekly_ap");
                    int weekly_gp = resultSet.getInt("weekly_gp");
                    int ap = resultSet.getInt("ap");
                    int gp = resultSet.getInt("gp");
                    int rank = resultSet.getInt("rank");
                    int top_ranking = resultSet.getInt("top_ranking");
                    int daily_kill = resultSet.getInt("daily_kill");
                    int weekly_kill = resultSet.getInt("weekly_kill");
                    int all_kill = resultSet.getInt("all_kill");
                    int max_rank = resultSet.getInt("max_rank");
                    int last_kill = resultSet.getInt("last_kill");
                    int last_ap = resultSet.getInt("last_ap");
                    int last_gp = resultSet.getInt("last_gp");
                    long last_update = resultSet.getLong("last_update");

                    abyssRank = new AbyssRank(daily_ap, daily_gp, weekly_ap, weekly_gp, ap, gp, rank, top_ranking, daily_kill, weekly_kill, all_kill, max_rank, last_kill, last_ap, last_gp, last_update);

                    abyssRank.setPersistentState(PersistentState.UPDATED);
                } else {
                    abyssRank = new AbyssRank(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, System.currentTimeMillis());
                    abyssRank.setPersistentState(PersistentState.NEW);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.210ed885e1c7", playerId, e));
        }

        return abyssRank;
    }

    /**
     * 加载并绑定玩家欧比斯军阶。
     * Loads and attaches abyss rank to the player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadAbyssRank(final Player player) {
        AbyssRank rank = loadAbyssRank(player.getObjectId());
        player.setAbyssRank(rank);
    }

    /**
     * 按持久化状态保存玩家欧比斯军阶。
     * Stores the player's abyss rank according to its persistent state.
     *
     * @param player 玩家 / player
     * @return 保存成功则为 true / true if stored successfully
     */
    @Override
    public boolean storeAbyssRank(Player player) {
        AbyssRank rank = player.getAbyssRank();
        try (Connection con = DatabaseFactory.getConnection()) {
            storeInTransaction(con, player.getObjectId(), rank);
            rank.setPersistentState(PersistentState.UPDATED);
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.418d26f689fd", player.getObjectId(), e));
            return false;
        }
    }

    @Override
    public void storeInTransaction(Connection con, int playerId, AbyssRank rank) throws SQLException {
        boolean result = switch (rank.getPersistentState()) {
            case NEW -> addRank(con, playerId, rank);
            case UPDATE_REQUIRED -> updateRank(con, playerId, rank);
            default -> true;
        };
        if (!result) {
            throw new SQLException("No abyss rank row changed for player " + playerId);
        }
    }

    /**
     * 插入新的欧比斯军阶记录。
     * Inserts a new abyss rank row.
     *
     * player object id
     * @param rank 欧比斯军阶 / abyss rank
     * true on success
     */
    private boolean addRank(Connection con, final int objectId, final AbyssRank rank) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, rank.getDailyAP());
            stmt.setInt(3, rank.getDailyGP());
            stmt.setInt(4, rank.getWeeklyAP());
            stmt.setInt(5, rank.getWeeklyGP());
            stmt.setInt(6, rank.getAp());
            stmt.setInt(7, rank.getGp());
            stmt.setInt(8, rank.getRank().getId());
            stmt.setInt(9, rank.getTopRanking());
            stmt.setInt(10, rank.getDailyKill());
            stmt.setInt(11, rank.getWeeklyKill());
            stmt.setInt(12, rank.getAllKill());
            stmt.setInt(13, rank.getMaxRank());
            stmt.setInt(14, rank.getLastKill());
            stmt.setInt(15, rank.getLastAP());
            stmt.setInt(16, rank.getLastGP());
            stmt.setLong(17, rank.getLastUpdate());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新已有欧比斯军阶记录。
     * Updates an existing abyss rank row.
     *
     * player object id
     * @param rank 欧比斯军阶 / abyss rank
     * true on success
     */
    private boolean updateRank(Connection con, final int objectId, final AbyssRank rank) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, rank.getDailyAP());
            stmt.setInt(2, rank.getDailyGP());
            stmt.setInt(3, rank.getWeeklyAP());
            stmt.setInt(4, rank.getWeeklyGP());
            stmt.setInt(5, rank.getAp());
            stmt.setInt(6, rank.getGp());
            stmt.setInt(7, rank.getRank().getId());
            stmt.setInt(8, rank.getTopRanking());
            stmt.setInt(9, rank.getDailyKill());
            stmt.setInt(10, rank.getWeeklyKill());
            stmt.setInt(11, rank.getAllKill());
            stmt.setInt(12, rank.getMaxRank());
            stmt.setInt(13, rank.getLastKill());
            stmt.setInt(14, rank.getLastAP());
            stmt.setInt(15, rank.getLastGP());
            stmt.setLong(16, rank.getLastUpdate());
            stmt.setInt(17, objectId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 获取指定阵营的玩家欧比斯排行榜。
     * Returns the player abyss ranking list for the given race.
     *
     * @param race 阵营 / race
     * @return 排行结果列表 / ranking results
     */
    @Override
    public ArrayList<AbyssRankingResult> getAbyssRankingPlayers(final Race race) {
        ArrayList<AbyssRankingResult> results = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYERS_RANKING)) {

            stmt.setString(1, race.toString());

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("players.name");
                    int playerAbyssRank = resultSet.getInt("abyss_rank.rank");
                    int ap = resultSet.getInt("abyss_rank.ap");
                    int gp = resultSet.getInt("abyss_rank.gp");
                    int playerTitle = resultSet.getInt("players.title_id");
                    int playerId = resultSet.getInt("players.id");
                    String playerClassStr = resultSet.getString("players.player_class");
                    int playerLevel = DataManager.PLAYER_EXPERIENCE_TABLE.getLevelForExp(resultSet.getLong("players.exp"));
                    String playerLegion = resultSet.getString("legions.name");
                    int oldRankPos = resultSet.getInt("old_rank_pos");
                    int rankPos = resultSet.getInt("rank_pos");

                    PlayerClass playerClass;
                    try {
                        playerClass = PlayerClass.getPlayerClassByString(playerClassStr);
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.9e67b184f360", playerClassStr));
                        continue;
                    }

                    if (playerClass != null) {
                        AbyssRankingResult rsl = new AbyssRankingResult(name, playerAbyssRank, playerId, ap, gp, playerTitle, playerClass, playerLevel, playerLegion, oldRankPos, rankPos);
                        results.add(rsl);
                    }
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.727699958569", race, e));
        }

        return results;
    }

    /**
     * 获取指定阵营的军团欧比斯排行榜。
     * Returns the legion abyss ranking list for the given race.
     *
     * @param race 阵营 / race
     * @return 排行结果列表 / ranking results
     */
    @Override
    public ArrayList<AbyssRankingResult> getAbyssRankingLegions(final Race race) {
        final ArrayList<AbyssRankingResult> results = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONS_RANKING)) {

            stmt.setString(1, race.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("legions.name");
                    long cp = rs.getLong("legions.contribution_points");
                    int legionId = rs.getInt("legions.id");
                    int legionLevel = rs.getInt("lvl");
                    int legionMembers = getLegionMembersCount(legionId);
                    int oldRankPos = rs.getInt("old_rank_pos");
                    int rankPos = rs.getInt("rank_pos");

                    AbyssRankingResult rsl = new AbyssRankingResult(cp, name, legionId, legionLevel, legionMembers, oldRankPos, rankPos);
                    results.add(rsl);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.feb137f3fb49", race, e));
        }

        return results;
    }

    /**
     * 统计军团成员数量。
     * Counts members of the given legion.
     *
     * legion id
     * member count
     */
    private int getLegionMembersCount(final int legionId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGION_COUNT)) {

            stmt.setInt(1, legionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("players");
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.d75f73bd7260", legionId, e));
        }

        return 0;
    }

    /**
     * 加载指定阵营玩家的 AP 映射；可限制活跃天数。
     * Loads player AP map for the race; optionally filters by offline days.
     *
     * 阵营 / race
     * lower AP limit
     * @param maxOfflineDays 最大离线天数，0 表示不过滤 / max offline days, 0 means no filter
     * @return 玩家 ID 到 AP 的映射 / map of player id to AP
     */
    @Override
    public Map<Integer, Integer> loadPlayersAp(final Race race, final int lowerApLimit, final int maxOfflineDays) {
        final Map<Integer, Integer> results = new HashMap<>();
        String query = maxOfflineDays > 0 ? SELECT_AP_PLAYER_ACTIVE_ONLY : SELECT_AP_PLAYER;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setString(1, race.toString());
            stmt.setInt(2, lowerApLimit);

            if (maxOfflineDays > 0) {
                stmt.setInt(3, maxOfflineDays);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    int ap = rs.getInt("ap");
                    results.put(playerId, ap);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.9546bd5b2940", race, e));
        }

        return results;
    }

    /**
     * 加载指定阵营玩家的 GP 映射；可限制活跃天数。
     * Loads player GP map for the race; optionally filters by offline days.
     *
     * 阵营 / race
     * lower GP limit
     * @param maxOfflineDays 最大离线天数，0 表示不过滤 / max offline days, 0 means no filter
     * @return 玩家 ID 到 GP 的映射 / map of player id to GP
     */
    @Override
    public Map<Integer, Integer> loadPlayersGp(final Race race, final int lowerGpLimit, final int maxOfflineDays) {
        final Map<Integer, Integer> results = new HashMap<>();
        String query = maxOfflineDays > 0 ? SELECT_GP_PLAYER_ACTIVE_ONLY : SELECT_GP_PLAYER;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setString(1, race.toString());
            stmt.setInt(2, lowerGpLimit);

            if (maxOfflineDays > 0) {
                stmt.setInt(3, maxOfflineDays);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    int gp = rs.getInt("gp");
                    results.put(playerId, gp);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.997ae7fa479a", race, e));
        }

        return results;
    }

    /**
     * 更新玩家欧比斯军阶枚举与名额。
     * Updates the player's abyss rank enum and top-ranking quota.
     *
     * player id
     * abyss rank enum
     */
    @Override
    public void updateAbyssRank(int playerId, AbyssRankEnum rankEnum) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_RANK)) {

            stmt.setInt(1, rankEnum.getId());
            stmt.setInt(2, rankEnum.getQuota());
            stmt.setInt(3, playerId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.77672b720fdd", playerId, e));
        }
    }

    /**
     * 重算双方阵营的玩家与军团排行位置。
     * Recalculates player and legion rank positions for both races.
     */
    @Override
    public void updateRankList() {
		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			try {

				// 更新天族玩家军阶 / Update Elyos player ranks
				try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
					 PreparedStatement stmt2 = con.prepareStatement(updatePlayerRankList())) {
					stmt1.execute();
					stmt2.setString(1, "ELYOS");
					stmt2.executeUpdate();
				}

				// 更新魔族玩家军阶 / Update Asmodian player ranks
				try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
					 PreparedStatement stmt2 = con.prepareStatement(updatePlayerRankList())) {
					stmt1.execute();
					stmt2.setString(1, "ASMODIANS");
					stmt2.executeUpdate();
				}

				// 更新天族军团军阶 / Update Elyos legion ranks
				try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
					 PreparedStatement stmt2 = con.prepareStatement(updateLegionRankList())) {
					stmt1.execute();
					stmt2.setString(1, "ELYOS");
					stmt2.executeUpdate();
				}

				// 更新魔族军团军阶 / Update Asmodian legion ranks
				try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
					 PreparedStatement stmt2 = con.prepareStatement(updateLegionRankList())) {
					stmt1.execute();
					stmt2.setString(1, "ASMODIANS");
					stmt2.executeUpdate();
				}

				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.4c4ea22ad393", e));
		}
    }

    /**
     * 批量删除玩家欧比斯军阶记录。
     * Batch-deletes abyss rank rows for the given players.
     *
     * @param listP 玩家列表 / player list
     */
    @Override
    public void removePlayer(List<Player> listP) {
        if (listP == null || listP.isEmpty()) {
            return;
        }

		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			try {
				try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
					for (Player player : listP) {
						stmt.setInt(1, player.getObjectId());
						stmt.addBatch();
					}
					stmt.executeBatch();
				}
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.19fd6d465738", e));
		}
    }

    /**
     * 是否支持该数据库。
     * Whether the database is supported.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     *
     * @return 若 supported 则为 true / true if supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
