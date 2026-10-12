package com.aionemu.gameserver.persistence.dao.impl;

import com.aionemu.gameserver.persistence.DatabaseAccess;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.dao.AbyssRankDao;
import com.aionemu.gameserver.persistence.entity.AbyssRankPo;
import com.aionemu.gameserver.persistence.mapper.AbyssRankMapper;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 欧比斯军阶数据访问实现。
 * Data access implementation for abyss rank.
 */
@Slf4j
@Service
@Lazy
public class AbyssRankDaoImpl extends ServiceImpl<AbyssRankMapper, AbyssRankPo> implements AbyssRankDao {

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
			Map<String, Object> row = mapper.selectRankByPlayerId(playerId);
			if (row == null) {
				return newRank(PersistentState.NEW);
			}
			AbyssRank rank = new AbyssRank(intValue(row, "daily_ap"), intValue(row, "daily_gp"),
				intValue(row, "weekly_ap"), intValue(row, "weekly_gp"), intValue(row, "ap"), intValue(row, "gp"),
				intValue(row, "rank"), intValue(row, "top_ranking"), intValue(row, "daily_kill"),
				intValue(row, "weekly_kill"), intValue(row, "all_kill"), intValue(row, "max_rank"),
				intValue(row, "last_kill"), intValue(row, "last_ap"), intValue(row, "last_gp"),
				longValue(row, "last_update"));
			rank.setPersistentState(PersistentState.UPDATED);
			return rank;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.210ed885e1c7", playerId), e);
			return newRank(PersistentState.NEW);
		}
	}

	/**
	 * 加载并绑定玩家欧比斯军阶。
	 * Loads and attaches abyss rank to the player.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void loadAbyssRank(Player player) {
		player.setAbyssRank(loadAbyssRank(player.getObjectId()));
	}

	/**
	 * 按持久化状态保存玩家欧比斯军阶。
	 * Stores the player's abyss rank according to its persistent state.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void storeAbyssRank(Player player) {
		AbyssRank rank = player.getAbyssRank();
		if (rank == null) {
			return;
		}
		PersistentState state = rank.getPersistentState();
		if (state != PersistentState.NEW && state != PersistentState.UPDATE_REQUIRED) {
			return;
		}
		try (Connection con = DatabaseAccess.getConnection()) {
			storeInTransaction(con, player.getObjectId(), rank);
			rank.setPersistentState(PersistentState.UPDATED);
		} catch (SQLException e) {
			log.error(I18n.get("log.418d26f689fd", player.getObjectId()), e);
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
			for (Map<String, Object> row : mapper.selectPlayerRanking(race.toString())) {
				String playerClassStr = stringValue(row, "player_class");
				PlayerClass playerClass;
				try {
					playerClass = PlayerClass.getPlayerClassByString(playerClassStr);
				} catch (IllegalArgumentException e) {
					log.warn(I18n.get("log.9e67b184f360", playerClassStr));
					continue;
				}
				if (playerClass == null) {
					continue;
				}
				int playerLevel = DataManager.PLAYER_EXPERIENCE_TABLE.getLevelForExp(longValue(row, "exp"));
				results.add(new AbyssRankingResult(stringValue(row, "player_name"), intValue(row, "rank"),
					intValue(row, "id"), intValue(row, "ap"), intValue(row, "gp"), intValue(row, "title_id"),
					playerClass, playerLevel, stringValue(row, "legion_name"), intValue(row, "old_rank_pos"),
					intValue(row, "rank_pos")));
			}
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
			for (Map<String, Object> row : mapper.selectLegionRanking(race.toString())) {
				int legionId = intValue(row, "id");
				results.add(new AbyssRankingResult(longValue(row, "contribution_points"), stringValue(row, "name"),
					legionId, intValue(row, "lvl"), getLegionMembersCount(legionId), intValue(row, "old_rank_pos"),
					intValue(row, "rank_pos")));
			}
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
			Integer count = mapper.countLegionMembers(legionId);
			return count == null ? 0 : count;
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

		try {
			List<Map<String, Object>> rows = maxOfflineDays > 0
				? mapper.selectActivePlayerGp(race.toString(), lowerGpLimit, maxOfflineDays)
				: mapper.selectPlayerGp(race.toString(), lowerGpLimit);
			for (Map<String, Object> row : rows) {
				results.put(intValue(row, "player_id"), intValue(row, "gp"));
			}
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
			mapper.updateRankAndQuota(rankEnum.getId(), rankEnum.getQuota(), playerId);
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
			DatabaseAccess.transactionTemplate().executeWithoutResult(_ -> {
				try (Connection con = DatabaseAccess.getConnection()) {
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
				mapper.deleteById(player.getObjectId());
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.19fd6d465738"), e);
		}
	}

	/**
	 * 依持久化状态插入军阶行（原生 PreparedStatement，随调用方事务）。
	 * Inserts the rank row by persistent state (raw PreparedStatement, caller's transaction).
	 */
	private boolean addRank(Connection con, final int objectId, final AbyssRank rank) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(AbyssRankMapper.INSERT_QUERY_JDBC)) {
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
	 * 依持久化状态更新军阶行（原生 PreparedStatement，随调用方事务）。
	 * Updates the rank row by persistent state (raw PreparedStatement, caller's transaction).
	 */
	private boolean updateRank(Connection con, final int objectId, final AbyssRank rank) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(AbyssRankMapper.UPDATE_QUERY_JDBC)) {
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
	 * 小缓存模式下限制玩家排行重算范围。
	 * Limits the player rank recalculation scope under small-cache mode.
	 */
	private static String updatePlayerRankList() {
		return AbyssRankMapper.UPDATE_PLAYER_RANK_LIST_JDBC + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 500" : "");
	}

	/**
	 * 小缓存模式下限制军团排行重算范围。
	 * Limits the legion rank recalculation scope under small-cache mode.
	 */
	private static String updateLegionRankList() {
		return AbyssRankMapper.UPDATE_LEGION_RANK_LIST_JDBC + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 75" : "");
	}

	/**
	 * 构造默认欧比斯军阶实例。
	 * Builds the default abyss rank instance.
	 */
	private static AbyssRank newRank(PersistentState state) {
		AbyssRank rank = new AbyssRank(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, System.currentTimeMillis());
		rank.setPersistentState(state);
		return rank;
	}

	/**
	 * 读取结果为 int，NULL 归零（对齐原 rs.getInt 语义）。
	 * Reads an int result, mapping NULL to zero (matching the original rs.getInt semantics).
	 */
	private static int intValue(Map<String, Object> row, String column) {
		Number value = (Number) row.get(column);
		return value == null ? 0 : value.intValue();
	}

	/**
	 * 读取结果为 long，NULL 归零（对齐原 rs.getLong 语义）。
	 * Reads a long result, mapping NULL to zero (matching the original rs.getLong semantics).
	 */
	private static long longValue(Map<String, Object> row, String column) {
		Number value = (Number) row.get(column);
		return value == null ? 0L : value.longValue();
	}

	/**
	 * 读取结果为 String，NULL 保持 null（对齐原 rs.getString 语义）。
	 * Reads a String result, keeping NULL as null (matching the original rs.getString semantics).
	 */
	private static String stringValue(Map<String, Object> row, String column) {
		Object value = row.get(column);
		return value == null ? null : value.toString();
	}
}
