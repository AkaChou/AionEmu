package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
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

import lombok.extern.slf4j.Slf4j;

/**
 * 欧比斯军阶数据访问对象的 MySQL 8 实现。
 * MySQL 8 implementation of AbyssRankDAO using JdbcClient.
 */
@Slf4j
@Repository
public class AbyssRankDAO extends com.aionemu.gameserver.dao.AbyssRankDAO {

	/**
	 * 按玩家 ID 查询欧比斯军阶 / Select abyss rank by player id
	 */
	public static final String SELECT_QUERY = "SELECT daily_ap, daily_gp, weekly_ap, weekly_gp, ap, gp, `rank`, " + "top_ranking, daily_kill, weekly_kill, all_kill, max_rank, last_kill, " + "last_ap, last_gp, last_update FROM abyss_rank WHERE player_id = ?";

	/**
	 * 插入欧比斯军阶记录 / Insert abyss rank row
	 */
	public static final String INSERT_QUERY = "INSERT INTO abyss_rank (player_id, daily_ap, daily_gp, weekly_ap, " + "weekly_gp, ap, gp, `rank`, top_ranking, daily_kill, weekly_kill, " + "all_kill, max_rank, last_kill, last_ap, last_gp, last_update) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	/**
	 * 更新欧比斯军阶记录 / Update abyss rank row
	 */
	public static final String UPDATE_QUERY = "UPDATE abyss_rank SET daily_ap = ?, daily_gp = ?, weekly_ap = ?, " + "weekly_gp = ?, ap = ?, gp = ?, `rank` = ?, top_ranking = ?, daily_kill = ?, " + "weekly_kill = ?, all_kill = ?, max_rank = ?, last_kill = ?, last_ap = ?, " + "last_gp = ?, last_update = ? WHERE player_id = ?";

	/**
	 * 查询阵营玩家欧比斯排行 / Select player abyss ranking for a race
	 */
	public static final String SELECT_PLAYERS_RANKING = "SELECT abyss_rank.`rank`, abyss_rank.ap, abyss_rank.gp, abyss_rank.old_rank_pos, " + "abyss_rank.rank_pos, players.name, legions.name, players.id, players.title_id, " + "players.player_class, players.exp FROM abyss_rank INNER JOIN players " + "ON abyss_rank.player_id = players.id LEFT JOIN legion_members " + "ON legion_members.player_id = players.id LEFT JOIN legions " + "ON legions.id = legion_members.legion_id WHERE players.race = ? " + "AND abyss_rank.gp > 1243 ORDER BY abyss_rank.gp DESC LIMIT 300";

	/**
	 * 查询阵营军团欧比斯排行 / Select legion abyss ranking for a race
	 */
	public static final String SELECT_LEGIONS_RANKING = "SELECT legions.id, legions.name, legions.contribution_points, " + "legions.level as lvl, legions.old_rank_pos, legions.rank_pos " + "FROM legions, legion_members, players WHERE players.race = ? " + "AND legion_members.`rank` = 'BRIGADE_GENERAL' AND legion_members.player_id = players.id " + "AND legion_members.legion_id = legions.id AND legions.contribution_points > 0 " + "GROUP BY id ORDER BY legions.contribution_points DESC LIMIT 50";

	/**
	 * 按阵营查询玩家 AP / Select player AP by race
	 */
	public static final String SELECT_AP_PLAYER = "SELECT player_id, ap FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND ap > ? " + "ORDER BY ap DESC";

	/**
	 * 按阵营查询活跃玩家 AP / Select active player AP by race
	 */
	public static final String SELECT_AP_PLAYER_ACTIVE_ONLY = "SELECT player_id, ap FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND ap > ? " + "AND UNIX_TIMESTAMP(CURDATE()) - UNIX_TIMESTAMP(players.last_online) <= ? * 24 * 60 * 60 " + "ORDER BY ap DESC";

	/**
	 * 按阵营查询玩家 GP / Select player GP by race
	 */
	public static final String SELECT_GP_PLAYER = "SELECT player_id, gp FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND gp > ? " + "ORDER BY gp DESC";

	/**
	 * 按阵营查询活跃玩家 GP / Select active player GP by race
	 */
	public static final String SELECT_GP_PLAYER_ACTIVE_ONLY = "SELECT player_id, gp FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND gp > ? " + "AND UNIX_TIMESTAMP(CURDATE()) - UNIX_TIMESTAMP(players.last_online) <= ? * 24 * 60 * 60 " + "ORDER BY gp DESC";

	/**
	 * 更新玩家军阶与名额 / Update player rank and top ranking quota
	 */
	public static final String UPDATE_RANK = "UPDATE abyss_rank SET `rank` = ?, top_ranking = ? WHERE player_id = ?";

	/**
	 * 统计军团成员数 / Count legion members
	 */
	public static final String SELECT_LEGION_COUNT = "SELECT COUNT(player_id) as players FROM legion_members WHERE legion_id = ?";

	/**
	 * 按 GP 重算玩家排行位置 / Recalculate player rank positions by GP
	 */
	public static final String UPDATE_PLAYER_RANK_LIST = "UPDATE abyss_rank SET abyss_rank.old_rank_pos = abyss_rank.rank_pos, " + "abyss_rank.rank_pos = @a:=@a+1 WHERE player_id IN " + "(SELECT id FROM players WHERE race = ?) ORDER BY gp DESC ";

	/**
	 * 按贡献点重算军团排行位置 / Recalculate legion rank positions by contribution
	 */
	public static final String UPDATE_LEGION_RANK_LIST = "UPDATE legions SET legions.old_rank_pos = legions.rank_pos, " + "legions.rank_pos = @a:=@a+1 WHERE id IN " + "(SELECT legion_id FROM legion_members, players WHERE legion_members.`rank` = 'BRIGADE_GENERAL' " + "AND players.id = legion_members.player_id AND players.race = ?) " + "ORDER BY legions.contribution_points DESC ";

	/**
	 * 删除玩家欧比斯军阶记录 / Delete player abyss rank row
	 */
	public static final String DELETE_QUERY = "DELETE FROM `abyss_rank` WHERE player_id = ?";

	private final JdbcClient jdbcClient;
	private final TransactionTemplate transactionTemplate;

	public AbyssRankDAO() {
		this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getTransactionTemplate());
	}

	@Autowired
	public AbyssRankDAO(JdbcClient jdbcClient, TransactionTemplate transactionTemplate) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
		this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate must not be null");
	}

	public AbyssRankDAO(JdbcClient jdbcClient) {
		this(jdbcClient, DatabaseFactory.getTransactionTemplate());
	}

	private static String updatePlayerRankList() {
		return UPDATE_PLAYER_RANK_LIST + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 500" : "");
	}

	private static String updateLegionRankList() {
		return UPDATE_LEGION_RANK_LIST + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 75" : "");
	}

	/**
	 * 按玩家 ID 加载欧比斯军阶；无记录时创建默认 NEW 状态实例。
	 * Loads abyss rank by player id; creates a default NEW instance when missing.
	 *
	 * @param playerId 玩家 ID / player id
	 * @return 欧比斯军阶 / abyss rank
	 */
	@Override
	public AbyssRank loadAbyssRank(int playerId) {
		try {
			AbyssRank rank = jdbcClient.sql(SELECT_QUERY)
				.param(playerId)
				.query((resultSet, _) -> {
					int daily_ap = resultSet.getInt("daily_ap");
					int daily_gp = resultSet.getInt("daily_gp");
					int weekly_ap = resultSet.getInt("weekly_ap");
					int weekly_gp = resultSet.getInt("weekly_gp");
					int ap = resultSet.getInt("ap");
					int gp = resultSet.getInt("gp");
					int r = resultSet.getInt("rank");
					int top_ranking = resultSet.getInt("top_ranking");
					int daily_kill = resultSet.getInt("daily_kill");
					int weekly_kill = resultSet.getInt("weekly_kill");
					int all_kill = resultSet.getInt("all_kill");
					int max_rank = resultSet.getInt("max_rank");
					int last_kill = resultSet.getInt("last_kill");
					int last_ap = resultSet.getInt("last_ap");
					int last_gp = resultSet.getInt("last_gp");
					long last_update = resultSet.getLong("last_update");

					AbyssRank ar = new AbyssRank(daily_ap, daily_gp, weekly_ap, weekly_gp, ap, gp, r, top_ranking, daily_kill, weekly_kill, all_kill, max_rank, last_kill, last_ap, last_gp, last_update);
					ar.setPersistentState(PersistentState.UPDATED);
					return ar;
				})
				.optional()
				.orElse(null);

			if (rank == null) {
				rank = new AbyssRank(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, System.currentTimeMillis());
				rank.setPersistentState(PersistentState.NEW);
			}
			return rank;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.210ed885e1c7", playerId), e);
			AbyssRank fallback = new AbyssRank(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, System.currentTimeMillis());
			fallback.setPersistentState(PersistentState.NEW);
			return fallback;
		}
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
	public void storeAbyssRank(Player player) {
		AbyssRank rank = player.getAbyssRank();
		if (rank == null) {
						;
		}
		PersistentState state = rank.getPersistentState();
		if (state != PersistentState.NEW && state != PersistentState.UPDATE_REQUIRED) {
						;
		}
		try (Connection con = DatabaseFactory.getConnection()) {
			storeInTransaction(con, player.getObjectId(), rank);
			rank.setPersistentState(PersistentState.UPDATED);
						;
		} catch (SQLException e) {
			log.error(I18n.get("log.418d26f689fd", player.getObjectId()), e);
						;
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

		try {
			jdbcClient.sql(SELECT_PLAYERS_RANKING)
				.param(race.toString())
				.query((resultSet, _) -> {
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
						return null;
					}

					if (playerClass != null) {
						results.add(new AbyssRankingResult(name, playerAbyssRank, playerId, ap, gp, playerTitle, playerClass, playerLevel, playerLegion, oldRankPos, rankPos));
					}
					return null;
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.727699958569", race), e);
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

		try {
			jdbcClient.sql(SELECT_LEGIONS_RANKING)
				.param(race.toString())
				.query((rs, _) -> {
					String name = rs.getString("legions.name");
					long cp = rs.getLong("legions.contribution_points");
					int legionId = rs.getInt("legions.id");
					int legionLevel = rs.getInt("lvl");
					int legionMembers = getLegionMembersCount(legionId);
					int oldRankPos = rs.getInt("old_rank_pos");
					int rankPos = rs.getInt("rank_pos");

					results.add(new AbyssRankingResult(cp, name, legionId, legionLevel, legionMembers, oldRankPos, rankPos));
					return null;
				})
				.list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.feb137f3fb49", race), e);
		}

		return results;
	}

	/**
	 * 统计军团成员数量。
	 * Counts members of the given legion.
	 *
	 * @param legionId 军团 ID / legion id
	 * @return 成员数 / member count
	 */
	private int getLegionMembersCount(final int legionId) {
		try {
			Integer count = jdbcClient.sql(SELECT_LEGION_COUNT)
				.param(legionId)
				.query(Integer.class)
				.optional()
				.orElse(0);
			return count;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d75f73bd7260", legionId), e);
			return 0;
		}
	}

	

	/**
	 * 加载指定阵营玩家的 GP 映射；可限制活跃天数。
	 * Loads player GP map for the race; optionally filters by offline days.
	 *
	 * @param race           阵营 / race
	 * @param lowerGpLimit   lower GP limit
	 * @param maxOfflineDays 最大离线天数，0 表示不过滤 / max offline days, 0 means no filter
	 * @return 玩家 ID 到 GP 的映射 / map of player id to GP
	 */
	@Override
	public Map<Integer, Integer> loadPlayersGp(final Race race, final int lowerGpLimit, final int maxOfflineDays) {
		final Map<Integer, Integer> results = new HashMap<>();
		String query = maxOfflineDays > 0 ? SELECT_GP_PLAYER_ACTIVE_ONLY : SELECT_GP_PLAYER;

		try {
			var spec = jdbcClient.sql(query)
				.param(race.toString())
				.param(lowerGpLimit);

			if (maxOfflineDays > 0) {
				spec.param(maxOfflineDays);
			}

			spec.query((rs, _) -> {
				int playerId = rs.getInt("player_id");
				int gp = rs.getInt("gp");
				results.put(playerId, gp);
				return null;
			}).list();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.997ae7fa479a", race), e);
		}

		return results;
	}

	/**
	 * 更新玩家欧比斯军阶枚举与名额。
	 * Updates the player's abyss rank enum and top-ranking quota.
	 *
	 * @param playerId 玩家 ID / player id
	 * @param rankEnum 欧比斯等级枚举 / abyss rank enum
	 */
	@Override
	public void updateAbyssRank(int playerId, AbyssRankEnum rankEnum) {
		try {
			jdbcClient.sql(UPDATE_RANK)
				.param(rankEnum.getId())
				.param(rankEnum.getQuota())
				.param(playerId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.77672b720fdd", playerId), e);
		}
	}

	/**
	 * 重算双方阵营的玩家与军团排行位置。
	 * Recalculates player and legion rank positions for both races.
	 */
	@Override
	public void updateRankList() {
		try {
			transactionTemplate.executeWithoutResult(_ -> {
				try (Connection con = DatabaseFactory.getConnection()) {
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
				} catch (SQLException e) {
					throw new DataAccessException("Failed to update abyss rank list", e) {};
				}
			});
		} catch (RuntimeException e) {
			log.error(I18n.get("log.4c4ea22ad393"), e);
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

		try {
			for (Player player : listP) {
				jdbcClient.sql(DELETE_QUERY)
					.param(player.getObjectId())
					.update();
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.19fd6d465738"), e);
		}
	}

	/**
	 * 是否支持该数据库。
	 * Whether the database is supported.
	 *
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
