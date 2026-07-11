package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABEntry;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家阿特雷亚图鉴 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerABDAO (Atreian Bestiary).
 *
 * @author Ranastic
 */
@Slf4j
public class MySQL8PlayerAtreianBestiaryDAO extends PlayerABDAO {


    /** 插入或更新图鉴 SQL / Insert or update bestiary SQL*/
    private static final String INSERT_OR_UPDATE = "INSERT INTO `player_atreian_bestiary` (`player_id`, `id`, `kill_count`, `level`, `claim_reward`) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE `id` = VALUES(`id`), `kill_count` = VALUES(`kill_count`), `level` = VALUES(`level`), `claim_reward` = VALUES(`claim_reward`)";

    /** 查询图鉴 SQL / Select bestiary SQL*/
    private static final String SELECT_QUERY = "SELECT `id`,`kill_count`,`level`,`claim_reward` FROM `player_atreian_bestiary` WHERE `player_id`=?";

    /** 删除图鉴条目 SQL / Delete bestiary entry SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";

    /**
     * 加载玩家图鉴列表。
     * Loads player Atreian bestiary list.
     *
     * 玩家 / player
     * bestiary list
     */
    @Override
    public PlayerABList load(Player player) {
        List<PlayerABEntry> cp = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("id");
                    int kill_count = rset.getInt("kill_count");
                    int level = rset.getInt("level");
                    int claimReward = rset.getInt("claim_reward");

                    cp.add(new PlayerABEntry(id, kill_count, level, claimReward, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.e26023dd4433", player.getObjectId(), e));
        }
        return new PlayerABList(cp);
    }

    /**
     * 存储图鉴条目。
     * Stores a bestiary entry.
     *
     * player object id
     * @param id 图鉴 ID / bestiary id
     * kill count
     * level
     * @param claimReward 是否已领奖 / claim reward flag
     * whether succeeded
     */
    @Override
    public boolean store(int objectId, int id, int kill_count, int level, int claimReward) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_OR_UPDATE)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, id);
            stmt.setInt(3, kill_count);
            stmt.setInt(4, level);
            stmt.setInt(5, claimReward);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.072ceacb9d9e", objectId, e));
            return false;
        }
    }

    /**
     * 删除图鉴条目。
     * Deletes a bestiary entry.
     *
     * player object id
     * @param id 图鉴 ID / bestiary id
     * whether succeeded
     */
    @Override
    public boolean delete(int playerObjId, int id) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, playerObjId);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.e05ecfff5330", playerObjId, e));
            return false;
        }
    }

    /**
     * 按 ID 获取击杀数。
     * Gets kill count by id.
     *
     * player object id
     * @param id 图鉴 ID / bestiary id
     * kill count
     */
    @Override
    public int getKillCountById(final int playerObjId, final int id) {
        String query = "SELECT `kill_count` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {

            s.setInt(1, playerObjId);
            s.setInt(2, id);

            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("kill_count");
                }
            }
        } catch (SQLException e) {
            log.debug("No kill count found for player {}, id {}", playerObjId, id);
        }
        return 0;
    }

    /**
     * 按 ID 获取等级。
     * Gets level by id.
     *
     * player object id
     * @param id 图鉴 ID / bestiary id
     * level
     */
    @Override
    public int getLevelById(final int playerObjId, final int id) {
        String query = "SELECT `level` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {

            s.setInt(1, playerObjId);
            s.setInt(2, id);

            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("level");
                }
            }
        } catch (SQLException e) {
            log.debug("No level found for player {}, id {}", playerObjId, id);
        }
        return 0;
    }

    /**
     * 按 ID 获取领奖状态。
     * Gets claim reward flag by id.
     *
     * player object id
     * @param id 图鉴 ID / bestiary id
     * claim reward flag
     */
    @Override
    public int getClaimRewardById(int playerObjId, int id) {
        String query = "SELECT `claim_reward` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {

            s.setInt(1, playerObjId);
            s.setInt(2, id);

            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("claim_reward");
                }
            }
        } catch (SQLException e) {
            log.debug("No claim reward found for player {}, id {}", playerObjId, id);
        }
        return 0;
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
