package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team.legion.LegionRank;

import lombok.extern.slf4j.Slf4j;

/**
 * 军团成员 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of LegionMemberDAO using JdbcClient.
 *
 * @author Simple
 * Updated for MySQL 8 - Fixed connection leaks and SQL syntax
 */
@Slf4j
@Repository
public class LegionMemberDAO extends com.aionemu.gameserver.dao.LegionMemberDAO {

	/**
	 * 插入军团成员 / Insert legion member
	 */
	private static final String INSERT_LEGIONMEMBER_QUERY = "INSERT INTO legion_members (`legion_id`, `player_id`, `rank`) VALUES (?, ?, ?)";

	/**
	 * 更新军团成员 / Update legion member
	 */
	private static final String UPDATE_LEGIONMEMBER_QUERY = "UPDATE legion_members SET nickname = ?, `rank` = ?, selfintro = ?, challenge_score = ? WHERE player_id = ?";

	/**
	 * 查询军团成员 / Select legion member
	 */
	private static final String SELECT_LEGIONMEMBER_QUERY = "SELECT * FROM legion_members WHERE player_id = ?";
	/**
	 * 删除军团成员 / Delete legion member
	 */
	private static final String DELETE_LEGIONMEMBER_QUERY = "DELETE FROM legion_members WHERE player_id = ?";
	/**
	 * 查询军团全部成员 ID / Select all member IDs of a legion
	 */
	private static final String SELECT_LEGIONMEMBERS_QUERY = "SELECT player_id FROM legion_members WHERE legion_id = ?";
	/**
	 * 按玩家 ID 查询扩展成员信息 / Select extended member info by player id
	 */
	private static final String SELECT_LEGIONMEMBEREX_QUERY = "SELECT players.name, players.exp, players.player_class, players.last_online, " + "players.world_id, legion_members.* FROM players, legion_members " + "WHERE players.id = ? AND players.id = legion_members.player_id";
	/**
	 * 按玩家名查询扩展成员信息 / Select extended member info by player name
	 */
	private static final String SELECT_LEGIONMEMBEREX2_QUERY = "SELECT players.id, players.exp, players.player_class, players.last_online, " + "players.world_id, legion_members.* FROM players, legion_members " + "WHERE players.name = ? AND players.id = legion_members.player_id";
	/**
	 * 检查玩家 ID 是否已在军团中 / Check whether player id is already used in legion
	 */
	private static final String CHECK_ID_USED_QUERY = "SELECT COUNT(player_id) as cnt FROM legion_members WHERE player_id = ?";
	/**
	 * 查询所有已使用的成员 ID / Select all used member player ids
	 */
	private static final String SELECT_USED_IDS_QUERY = "SELECT player_id FROM legion_members";

	private final JdbcClient jdbcClient;

	public LegionMemberDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public LegionMemberDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 判断指定玩家 ID 是否已作为军团成员使用。
	 * Checks whether the given player object id is already used as a legion member.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 是否已使用 / whether used
	 */
	@Override
	public boolean isIdUsed(final int playerObjId) {
		try {
			return jdbcClient.sql(CHECK_ID_USED_QUERY)
				.param(playerObjId)
				.query((rs, _) -> rs.getInt("cnt") > 0)
				.optional()
				.orElse(false);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.808625122cbd", playerObjId), e);
			return true;
		}
	}

	/**
	 * 保存新的军团成员记录。
	 * Saves a newly created legion member record.
	 *
	 * @param legionMember 军团成员 / legion member
	 */
	@Override
	public void saveNewLegionMember(final LegionMember legionMember) {
		try {
			jdbcClient.sql(INSERT_LEGIONMEMBER_QUERY)
				.param(legionMember.getLegion().getLegionId())
				.param(legionMember.getObjectId())
				.param(legionMember.getRank().toString())
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.9e9361f5deaa", legionMember.getObjectId()), e);
		}
	}

	/**
	 * 更新指定玩家的军团成员信息。
	 * Updates the legion member info for the given player.
	 *
	 * @param playerId     玩家 ID / player id
	 * @param legionMember 军团成员数据 / legion member data
	 */
	@Override
	public void storeLegionMember(final int playerId, final LegionMember legionMember) {
		try {
			jdbcClient.sql(UPDATE_LEGIONMEMBER_QUERY)
				.param(legionMember.getNickname())
				.param(legionMember.getRank().toString())
				.param(legionMember.getSelfIntro())
				.param(legionMember.getChallengeScore())
				.param(playerId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a9693496afec", playerId), e);
		}
	}

	/**
	 * 按玩家对象 ID 加载军团成员。
	 * Loads a legion member by player object id.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 军团成员，不存在则为 null / legion member or null
	 */
	@Override
	public LegionMember loadLegionMember(final int playerObjId) {
		if (playerObjId == 0) {
			return null;
		}

		try {
			return jdbcClient.sql(SELECT_LEGIONMEMBER_QUERY)
				.param(playerObjId)
				.query((resultSet, _) -> {
					LegionMember legionMember = new LegionMember(playerObjId);
					int legionId = resultSet.getInt("legion_id");

					try {
						legionMember.setRank(LegionRank.valueOf(resultSet.getString("rank")));
					} catch (IllegalArgumentException e) {
						log.warn(I18n.get("log.70eb91ce3a9a", playerObjId, resultSet.getString("rank")));
						legionMember.setRank(LegionRank.VOLUNTEER);
					}

					legionMember.setNickname(resultSet.getString("nickname"));
					legionMember.setSelfIntro(resultSet.getString("selfintro"));
					legionMember.setChallengeScore(resultSet.getInt("challenge_score"));
					legionMember.setLegion(GameCoreGameplayServices.legionService().getLegion(legionId));

					return legionMember;
				})
				.optional()
				.orElse(null);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b98d06a4d45a", playerObjId), e);
			return null;
		}
	}

	/**
	 * 按玩家对象 ID 加载扩展军团成员信息（含玩家基础数据）。
	 * Loads extended legion member info by player object id (includes player base data).
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 * @return 扩展军团成员，不存在则为 null / extended legion member or null
	 */
	@Override
	public LegionMemberEx loadLegionMemberEx(final int playerObjId) {
		try {
			return jdbcClient.sql(SELECT_LEGIONMEMBEREX_QUERY)
				.param(playerObjId)
				.query((resultSet, _) -> {
					LegionMemberEx legionMemberEx = new LegionMemberEx(playerObjId);

					legionMemberEx.setName(resultSet.getString("players.name"));
					legionMemberEx.setExp(resultSet.getLong("players.exp"));

					try {
						legionMemberEx.setPlayerClass(PlayerClass.valueOf(resultSet.getString("players.player_class")));
					} catch (IllegalArgumentException e) {
						log.warn(I18n.get("log.40c87a64d2f0", playerObjId, resultSet.getString("players.player_class")));
					}

					legionMemberEx.setLastOnline(resultSet.getTimestamp("players.last_online"));
					legionMemberEx.setWorldId(resultSet.getInt("players.world_id"));

					int legionId = resultSet.getInt("legion_members.legion_id");

					try {
						legionMemberEx.setRank(LegionRank.valueOf(resultSet.getString("legion_members.rank")));
					} catch (IllegalArgumentException e) {
						log.warn(I18n.get("log.70eb91ce3a9a", playerObjId, resultSet.getString("legion_members.rank")));
						legionMemberEx.setRank(LegionRank.VOLUNTEER);
					}

					legionMemberEx.setNickname(resultSet.getString("legion_members.nickname"));
					legionMemberEx.setSelfIntro(resultSet.getString("legion_members.selfintro"));
					legionMemberEx.setLegion(GameCoreGameplayServices.legionService().getLegion(legionId));

					return legionMemberEx;
				})
				.optional()
				.orElse(null);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.dfcfd04a2c84", playerObjId), e);
			return null;
		}
	}

	/**
	 * 按玩家名加载扩展军团成员信息。
	 * Loads extended legion member info by player name.
	 *
	 * @param playerName 玩家名 / player name
	 * @return 扩展军团成员，不存在则为 null / extended legion member or null
	 */
	@Override
	public LegionMemberEx loadLegionMemberEx(final String playerName) {
		try {
			return jdbcClient.sql(SELECT_LEGIONMEMBEREX2_QUERY)
				.param(playerName)
				.query((resultSet, _) -> {
					LegionMemberEx legionMember = new LegionMemberEx(playerName);

					legionMember.setObjectId(resultSet.getInt("id"));
					legionMember.setExp(resultSet.getLong("exp"));

					try {
						legionMember.setPlayerClass(PlayerClass.valueOf(resultSet.getString("player_class")));
					} catch (IllegalArgumentException e) {
						log.warn(I18n.get("log.40c87a64d2f0", playerName, resultSet.getString("player_class")));
					}

					legionMember.setLastOnline(resultSet.getTimestamp("last_online"));
					legionMember.setWorldId(resultSet.getInt("world_id"));

					int legionId = resultSet.getInt("legion_id");

					try {
						legionMember.setRank(LegionRank.valueOf(resultSet.getString("rank")));
					} catch (IllegalArgumentException e) {
						log.warn(I18n.get("log.70eb91ce3a9a", playerName, resultSet.getString("rank")));
						legionMember.setRank(LegionRank.VOLUNTEER);
					}

					legionMember.setNickname(resultSet.getString("nickname"));
					legionMember.setSelfIntro(resultSet.getString("selfintro"));
					legionMember.setLegion(GameCoreGameplayServices.legionService().getLegion(legionId));

					return legionMember;
				})
				.optional()
				.orElse(null);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.29172f0eb8e2", playerName), e);
			return null;
		}
	}

	/**
	 * 加载指定军团的全部成员玩家 ID 列表。
	 * Loads all member player ids for the given legion.
	 *
	 * @param legionId 军团 ID / legion id
	 * @return 成员 ID 列表，无成员则为 null / member id list or null
	 */
	@Override
	public ArrayList<Integer> loadLegionMembers(final int legionId) {
		final ArrayList<Integer> legionMembers = new ArrayList<>();

		try {
			legionMembers.addAll(jdbcClient.sql(SELECT_LEGIONMEMBERS_QUERY)
				.param(legionId)
				.query((resultSet, _) -> resultSet.getInt("player_id"))
				.list());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.580dbf1f5523", legionId), e);
		}

		return legionMembers.isEmpty() ? null : legionMembers;
	}

	/**
	 * 删除指定玩家的军团成员记录。
	 * Deletes the legion member record for the given player.
	 *
	 * @param playerObjId 玩家对象 ID / player object id
	 */
	@Override
	public void deleteLegionMember(int playerObjId) {
		try {
			jdbcClient.sql(DELETE_LEGIONMEMBER_QUERY)
				.param(playerObjId)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.d5662c3ac841", playerObjId), e);
		}
	}

	/**
	 * 判断当前数据库是否受本 DAO 支持。
	 * Checks whether the given database is supported by this DAO.
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

	/**
	 * 获取所有已使用的军团成员玩家 ID。
	 * Returns all used legion member player ids.
	 *
	 * @return 已使用 ID 数组 / used id array.
	 */
	@Override
	public int[] getUsedIDs() {
		try {
			List<Integer> ids = jdbcClient.sql(SELECT_USED_IDS_QUERY)
				.query((rs, _) -> rs.getInt("player_id"))
				.list();

			int[] result = new int[ids.size()];
			for (int i = 0; i < ids.size(); i++) {
				result[i] = ids.get(i);
			}
			return result;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.fec518ccee9a"), e);
			return new int[0];
		}
	}
}
