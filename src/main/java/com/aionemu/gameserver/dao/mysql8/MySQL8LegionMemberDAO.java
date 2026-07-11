package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.team.legion.LegionMember;
import com.aionemu.gameserver.model.team.legion.LegionMemberEx;
import com.aionemu.gameserver.model.team.legion.LegionRank;
import java.sql.*;
import java.util.ArrayList;

/**
 * 军团成员 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of LegionMemberDAO.
 *
 * @author Simple
 * Updated for MySQL 8 - Fixed connection leaks and SQL syntax
 */
@Slf4j
public class MySQL8LegionMemberDAO extends LegionMemberDAO {


    /** 插入军团成员 / Insert legion member */
    private static final String INSERT_LEGIONMEMBER_QUERY = "INSERT INTO legion_members (`legion_id`, `player_id`, `rank`) VALUES (?, ?, ?)";

    /** 更新军团成员 / Update legion member */
    private static final String UPDATE_LEGIONMEMBER_QUERY = "UPDATE legion_members SET nickname = ?, `rank` = ?, selfintro = ?, challenge_score = ? WHERE player_id = ?";

    /** 查询军团成员 / Select legion member */
    private static final String SELECT_LEGIONMEMBER_QUERY = "SELECT * FROM legion_members WHERE player_id = ?";
    /** 删除军团成员 / Delete legion member */
    private static final String DELETE_LEGIONMEMBER_QUERY = "DELETE FROM legion_members WHERE player_id = ?";
    /** 查询军团全部成员 ID / Select all member IDs of a legion */
    private static final String SELECT_LEGIONMEMBERS_QUERY = "SELECT player_id FROM legion_members WHERE legion_id = ?";
    /** 按玩家 ID 查询扩展成员信息 / Select extended member info by player id */
    private static final String SELECT_LEGIONMEMBEREX_QUERY = "SELECT players.name, players.exp, players.player_class, players.last_online, " + "players.world_id, legion_members.* FROM players, legion_members " + "WHERE players.id = ? AND players.id = legion_members.player_id";
    /** 按玩家名查询扩展成员信息 / Select extended member info by player name */
    private static final String SELECT_LEGIONMEMBEREX2_QUERY = "SELECT players.id, players.exp, players.player_class, players.last_online, " + "players.world_id, legion_members.* FROM players, legion_members " + "WHERE players.name = ? AND players.id = legion_members.player_id";
    /** 检查玩家 ID 是否已在军团中 / Check whether player id is already used in legion */
    private static final String CHECK_ID_USED_QUERY = "SELECT COUNT(player_id) as cnt FROM legion_members WHERE player_id = ?";
    /** 查询所有已使用的成员 ID / Select all used member player ids */
    private static final String SELECT_USED_IDS_QUERY = "SELECT player_id FROM legion_members";

    /**
     * 判断指定玩家 ID 是否已作为军团成员使用。
     * Checks whether the given player object id is already used as a legion member.
     *
     * player object id
     *
     * @param playerObjId @return 是否已使用 / whether used
     */
    @Override
    public boolean isIdUsed(final int playerObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(CHECK_ID_USED_QUERY)) {

            s.setInt(1, playerObjId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            log.error(I18n.get("log.808625122cbd", playerObjId, e));
            return true;
        }
    }

    /**
     * 保存新的军团成员记录。
     * Saves a newly created legion member record.
     *
     * legion member
     *
     * @param legionMember @return 是否保存成功 / whether saved successfully
     */
    @Override
    public boolean saveNewLegionMember(final LegionMember legionMember) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_LEGIONMEMBER_QUERY)) {

            stmt.setInt(1, legionMember.getLegion().getLegionId());
            stmt.setInt(2, legionMember.getObjectId());
            stmt.setString(3, legionMember.getRank().toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.9e9361f5deaa", legionMember.getObjectId(), e));
            return false;
        }
    }

    /**
     * 更新指定玩家的军团成员信息。
     * Updates the legion member info for the given player.
     *
     * player id
     * @param legionMember 军团成员数据 / legion member data
     */
    @Override
    public void storeLegionMember(final int playerId, final LegionMember legionMember) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LEGIONMEMBER_QUERY)) {

            stmt.setString(1, legionMember.getNickname());
            stmt.setString(2, legionMember.getRank().toString()); // rank экранирован в запросе
            stmt.setString(3, legionMember.getSelfIntro());
            stmt.setInt(4, legionMember.getChallengeScore());
            stmt.setInt(5, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.a9693496afec", playerId, e));
        }
    }

    /**
     * 按玩家对象 ID 加载军团成员。
     * Loads a legion member by player object id.
     *
     * player object id
     *
     * @param playerObjId @return 军团成员，不存在则为 null / legion member or null
     */
    @Override
    public LegionMember loadLegionMember(final int playerObjId) {
        if (playerObjId == 0) {
            return null;
        }

        LegionMember result = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBER_QUERY)) {

            stmt.setInt(1, playerObjId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
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

                    result = legionMember;
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.b98d06a4d45a", playerObjId, e));
        }

        return result;
    }

    /**
     * 按玩家对象 ID 加载扩展军团成员信息（含玩家基础数据）。
     * Loads extended legion member info by player object id (includes player base data).
     *
     * player object id
     *
     * @param playerObjId @return 扩展军团成员，不存在则为 null / extended legion member or null
     */
    @Override
    public LegionMemberEx loadLegionMemberEx(final int playerObjId) {
        LegionMemberEx result = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBEREX_QUERY)) {

            stmt.setInt(1, playerObjId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
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

                    result = legionMemberEx;
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.dfcfd04a2c84", playerObjId, e));
        }

        return result;
    }

    /**
     * 按玩家名加载扩展军团成员信息。
     * Loads extended legion member info by player name.
     *
     * player name
     *
     * @param playerName @return 扩展军团成员，不存在则为 null / extended legion member or null
     */
    @Override
    public LegionMemberEx loadLegionMemberEx(final String playerName) {
        LegionMemberEx result = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBEREX2_QUERY)) {

            stmt.setString(1, playerName);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
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

                    result = legionMember;
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.29172f0eb8e2", playerName, e));
        }

        return result;
    }

    /**
     * 加载指定军团的全部成员玩家 ID 列表。
     * Loads all member player ids for the given legion.
     *
     * legion id
     *
     * @param legionId @return 成员 ID 列表，无成员则为 null / member id list or null
     */
    @Override
    public ArrayList<Integer> loadLegionMembers(final int legionId) {
        final ArrayList<Integer> legionMembers = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONMEMBERS_QUERY)) {

            stmt.setInt(1, legionId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    int playerObjId = resultSet.getInt("player_id");
                    legionMembers.add(playerObjId);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.580dbf1f5523", legionId, e));
        }

        return legionMembers.isEmpty() ? null : legionMembers;
    }

    /**
     * 删除指定玩家的军团成员记录。
     * Deletes the legion member record for the given player.
     *
     * player object id
     */
    @Override
    public void deleteLegionMember(int playerObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(DELETE_LEGIONMEMBER_QUERY)) {

            statement.setInt(1, playerObjId);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.d5662c3ac841", playerObjId, e));
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    /**
     * 获取所有已使用的军团成员玩家 ID。
     * Returns all used legion member player ids.
     *
     * 已使用 ID 数组。
     * used id array.
     */
    @Override
    public int[] getUsedIDs() {
        ArrayList<Integer> ids = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_USED_IDS_QUERY,
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getInt("player_id"));
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.fec518ccee9a", e));
            return new int[0];
        }

        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }
}
