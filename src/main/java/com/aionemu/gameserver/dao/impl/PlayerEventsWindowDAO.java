package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.event_window.PlayerEventWindowEntry;
import com.aionemu.gameserver.model.event_window.PlayerEventWindowList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家活动窗口进度 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerEventsWindowDAO.
 *
 * @author Ghostfur (Aion-Unique)
 * Updated for MySQL 8 - Fixed connection leaks
 */
@Slf4j
public class PlayerEventsWindowDAO extends com.aionemu.gameserver.dao.PlayerEventsWindowDAO {

    /** 按账号加载活动窗口 / Select event windows by account */
    private static final String SELECT_QUERY = "SELECT * FROM `player_events_window` WHERE `account_id`=?";
    /** 插入或更新活动窗口 / Insert or update event window */
    private static final String INSERT_QUERY = "INSERT INTO `player_events_window` (`account_id`, `event_id`, `last_stamp`, `elapsed`) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE `event_id` = VALUES(`event_id`), `last_stamp` = VALUES(`last_stamp`)";
    /** 简单插入活动窗口 / Simple insert event window */
    private static final String INSERT_SIMPLE_QUERY = "INSERT INTO `player_events_window` (`account_id`, `event_id`, `last_stamp`) VALUES (?,?,?)";
    /** 删除指定活动窗口 / Delete an event window entry */
    private static final String DELETE_QUERY = "DELETE FROM player_events_window WHERE account_id = ? AND event_id = ?";
    /** 查询账号下全部活动 ID / Select all event ids for account */
    private static final String SELECT_IDS_QUERY = "SELECT event_id FROM player_events_window WHERE account_id = ?";
    /** 查询上次时间戳 / Select last stamp */
    private static final String SELECT_LAST_STAMP_QUERY = "SELECT last_stamp FROM player_events_window WHERE account_id = ? AND event_id = ?";
    /** 查询已过时间 / Select elapsed time */
    private static final String SELECT_ELAPSED_QUERY = "SELECT elapsed FROM player_events_window WHERE account_id = ? AND event_id = ?";
    /** 更新已过时间 / Update elapsed time */
    private static final String UPDATE_ELAPSED_QUERY = "UPDATE player_events_window SET elapsed = ? WHERE account_id = ? AND event_id = ?";
    /** 查询已领奖励次数 / Select reward received count */
    private static final String SELECT_REWARD_COUNT_QUERY = "SELECT reward_recived_count FROM player_events_window WHERE account_id = ? AND event_id = ?";
    /** 更新奖励次数并重置进度 / Update reward count and reset progress */
    private static final String UPDATE_REWARD_QUERY = "UPDATE player_events_window SET reward_recived_count = ?, elapsed = 0, last_stamp = NOW() WHERE account_id = ? AND event_id = ?";

    /**
     * 加载玩家的活动窗口进度列表。
     * Loads the player's event window progress list.
     *
     * @param player 玩家 / player
     * @return 活动窗口列表 / event window list
     */
    @Override
    public PlayerEventWindowList load(Player player) {
        List<PlayerEventWindowEntry> eventWindow = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getPlayerAccount().getId());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("event_id");
                    Timestamp lastStamp = rset.getTimestamp("last_stamp");
                    int elapsed = rset.getInt("elapsed");
                    eventWindow.add(new PlayerEventWindowEntry(id, lastStamp, elapsed, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.934e7f9ba605", player.getObjectId(), e));
        }
        return new PlayerEventWindowList(eventWindow);
    }

    /**
     * 存储活动窗口进度。
     * Stores event window progress.
     *
     * 账号 ID / account id
     * event id
     * @param last_stamp 上次时间戳 / last stamp
     * elapsed time
     * whether successful
     */
    @Override
    public boolean store(int accountId, int eventId, Timestamp last_stamp, int elapsed) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            stmt.setTimestamp(3, last_stamp);
            stmt.setInt(4, elapsed);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.d5794895e11e", accountId, e));
            return false;
        }
    }

    /**
     * 插入一条活动窗口记录。
     * Inserts an event window record.
     *
     * 账号 ID / account id
     * event id
     * @param last_stamp 上次时间戳 / last stamp
     */
    @Override
    public void insert(int accountId, int eventId, Timestamp last_stamp) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_SIMPLE_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            stmt.setTimestamp(3, last_stamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.57c352a40e1f", e));
        }
    }

    /**
     * 删除指定账号的活动窗口记录。
     * Deletes an event window record for the account.
     *
     * 账号 ID / account id
     * event id
     */
    @Override
    public void delete(final int accountId, final int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.10a9343136b2", accountId, eventId, e));
        }
    }

    /**
     * 获取账号下全部活动窗口 ID。
     * Returns all event window ids for the account.
     *
     * 账号 ID / account id
     * list of event ids
     */
    @Override
    public List<Integer> getEventsWindow(final int accountId) {
        final List<Integer> ids = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_IDS_QUERY)) {

            stmt.setInt(1, accountId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("event_id"));
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.55105d71a66c", accountId, e));
        }
        return ids;
    }

    /**
     * 获取指定活动的上次时间戳。
     * Returns the last stamp for the given event.
     *
     * 账号 ID / account id
     * event id
     * @return 上次时间戳；无记录时返回当前时间 / last stamp, or now if none
     */
    @Override
    public Timestamp getLastStamp(int accountId, int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LAST_STAMP_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("last_stamp");
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.14716135e108", accountId, eventId, e));
        }
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * 获取指定活动已过时间。
     * Returns the elapsed time for the given event.
     *
     * 账号 ID / account id
     * event id
     * @return 已过时间；无记录时为 0 / elapsed time, or 0 if none
     */
    @Override
    public int getElapsed(int accountId, int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ELAPSED_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("elapsed");
                }
            }
        } catch (SQLException e) {
            log.debug("No elapsed time found for account {}, event {}", accountId, eventId);
        }
        return 0;
    }

    /**
     * 更新指定活动的已过时间。
     * Updates the elapsed time for the given event.
     *
     * 账号 ID / account id
     * event id
     * elapsed time
     */
    @Override
    public void updateElapsed(int accountId, int eventId, int elapsed) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ELAPSED_QUERY)) {

            stmt.setInt(1, elapsed);
            stmt.setInt(2, accountId);
            stmt.setInt(3, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.59f40fb9f03a", accountId, eventId, e));
        }
    }

    /**
     * 获取指定活动已领取奖励次数。
     * Returns the reward received count for the given event.
     *
     * 账号 ID / account id
     * event id
     *
     * @return 已领次数；无记录时为 0 / reward count, or 0 if none
     */
    @Override
    public int getRewardRecivedCount(int accountId, int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_REWARD_COUNT_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("reward_recived_count ");
                }
            }
        } catch (SQLException e) {
            log.debug("No reward count found for account {}, event {}", accountId, eventId);
        }
        return 0;
    }

    /**
     * 设置已领取奖励次数并重置进度。
     * Sets the reward received count and resets progress.
     *
     * 账号 ID / account id
     * event id
     * @param rewardReceivedCount 已领奖励次数 / reward received count
     */
    @Override
    public void setRewardRecivedCount(int accountId, int eventId, int rewardReceivedCount) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_REWARD_QUERY)) {

            stmt.setInt(1, rewardReceivedCount);
            stmt.setInt(2, accountId);
            stmt.setInt(3, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.90532cef7c99", accountId, eventId, e));
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
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
