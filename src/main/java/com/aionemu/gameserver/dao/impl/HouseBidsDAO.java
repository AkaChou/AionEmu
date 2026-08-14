package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.house.PlayerHouseBid;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 房屋竞拍出价 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of HouseBidsDAO.
 *
 * @author Rolandas, Updated for MySQL 8
 */
@Slf4j
public class HouseBidsDAO extends com.aionemu.gameserver.dao.HouseBidsDAO {

    /** 加载全部出价（按房屋与金额排序） / Load all bids ordered by house and bid */
    private static final String LOAD_QUERY = "SELECT * FROM `house_bids` ORDER BY `house_id`, `bid` DESC";
    /** 新增出价 / Insert a bid */
    private static final String INSERT_QUERY = "INSERT INTO `house_bids` (`player_id`, `house_id`, `bid`, `bid_time`) VALUES (?, ?, ?, ?)";
    /** 删除指定房屋全部出价 / Delete all bids for a house */
    private static final String DELETE_QUERY = "DELETE FROM `house_bids` WHERE `house_id` = ?";
    /** 删除玩家对指定房屋的出价 / Delete a player's bid for a house */
    private static final String DELETE_PLAYER_QUERY = "DELETE FROM `house_bids` WHERE `player_id` = ? AND `house_id` = ?";
    /** 更新出价金额与时间 / Update bid amount and time */
    private static final String UPDATE_QUERY = "UPDATE `house_bids` SET `bid` = ?, `bid_time` = ? WHERE `player_id` = ? AND `house_id` = ?";
    /** 查询房屋最高出价 / Select highest bid for a house */
    private static final String SELECT_HIGHEST_BID_QUERY = "SELECT MAX(`bid`) FROM `house_bids` WHERE `house_id` = ?";
    /** 查询玩家对指定房屋的出价 / Select a player's bid for a house */
    private static final String SELECT_PLAYER_BID_QUERY = "SELECT `bid`, `bid_time` FROM `house_bids` WHERE `player_id` = ? AND `house_id` = ?";

    /**
     * 加载全部房屋竞拍出价。
     * Loads all house auction bids.
     *
     * @return 出价集合 / set of bids
     */
    @Override
    public Set<PlayerHouseBid> loadBids() {
        Set<PlayerHouseBid> bids = new HashSet<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOAD_QUERY);
             ResultSet rset = stmt.executeQuery()) {

            while (rset.next()) {
                int playerId = rset.getInt("player_id");
                int houseId = rset.getInt("house_id");
                long bidOffer = rset.getLong("bid");
                Timestamp time = rset.getTimestamp("bid_time");

                PlayerHouseBid bid = new PlayerHouseBid(playerId, houseId, bidOffer, time);
                bids.add(bid);
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.ff14dcd21c64", e));
        }

        return bids;
    }

    /**
     * 新增一条房屋竞拍出价。
     * Adds a new house auction bid.
     *
     * @param playerId 玩家 ID / player id
     * @param houseId 房屋 ID / house id
     * @param bidOffer 出价金额 / bid amount
     * @param time 出价时间 / bid time
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean addBid(int playerId, int houseId, long bidOffer, Timestamp time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, houseId);
            stmt.setLong(3, bidOffer);
            stmt.setTimestamp(4, time);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.f1c5d89c6500", playerId, houseId, e));
            return false;
        }
    }

    /**
     * 变更玩家对房屋的出价；若不存在则插入。
     * Changes a player's bid for a house; inserts if none exists.
     *
     * @param playerId 玩家 ID / player id
     * @param houseId 房屋 ID / house id
     * @param newBidOffer 新出价金额 / new bid amount
     * @param time 出价时间 / bid time
     */
    @Override
    public void changeBid(int playerId, int houseId, long newBidOffer, Timestamp time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setLong(1, newBidOffer);
            stmt.setTimestamp(2, time);
            stmt.setInt(3, playerId);
            stmt.setInt(4, houseId);

            int updated = stmt.executeUpdate();

            if (updated == 0) {
                addBid(playerId, houseId, newBidOffer, time);
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.68c1f0c325d4", playerId, houseId, e));
        }
    }

    /**
     * 删除指定房屋的全部出价。
     * Deletes all bids for the given house.
     *
     * @param houseId 房屋 ID / house id
     */
    @Override
    public void deleteHouseBids(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, houseId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.913c026c08a6", houseId, e));
        }
    }

    /**
     * 删除玩家对指定房屋的出价。
     * Deletes a player's bid for the given house.
     *
     * @param playerId 玩家 ID / player id
     * @param houseId 房屋 ID / house id
     */
    public void deletePlayerBid(int playerId, int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_PLAYER_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, houseId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.d16d4a0faede", playerId, houseId, e));
        }
    }

    /**
     * 获取指定房屋的最高出价。
     * Returns the highest bid for the given house.
     *
     * house id
     *
     * @param houseId 房屋 ID / house id
     * @return 最高出价；无记录时为 0 / highest bid, or 0 if none
     */
    public long getHighestBid(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_HIGHEST_BID_QUERY)) {

            stmt.setInt(1, houseId);

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getLong(1);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.215d2f2d1d7e", houseId, e));
        }

        return 0;
    }

    /**
     * 获取玩家对指定房屋的出价。
     * Returns the player's bid for the given house.
     *
     * @param playerId 玩家 ID / player id
     * @param houseId 房屋 ID / house id
     *
     * @return 出价记录；不存在返回 null / bid record, or null if none
     */
    public PlayerHouseBid getPlayerBid(int playerId, int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_BID_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, houseId);

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    long bid = rset.getLong("bid");
                    Timestamp time = rset.getTimestamp("bid_time");
                    return new PlayerHouseBid(playerId, houseId, bid, time);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.fe00b8086073", playerId, houseId, e));
        }

        return null;
    }

    /**
     * 批量删除多套房屋的出价。
     * Batch-deletes bids for multiple houses.
     *
     * @param houseIds 房屋 ID 集合 / set of house ids
     */
    public void deleteHouseBids(Set<Integer> houseIds) {
        if (houseIds == null || houseIds.isEmpty()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
                for (int houseId : houseIds) {
                    stmt.setInt(1, houseId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            con.commit();

        } catch (SQLException e) {
            log.error(I18n.get("log.13c5e65ca1ad", e));
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
}
