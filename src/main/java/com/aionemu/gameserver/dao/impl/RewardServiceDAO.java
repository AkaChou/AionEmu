package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Web 奖励服务 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of RewardServiceDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class RewardServiceDAO extends com.aionemu.gameserver.dao.RewardServiceDAO {

    /** 标记奖励为已领取 SQL / Mark reward as received SQL*/
    private static final String UPDATE_QUERY = "UPDATE `web_reward` SET `rewarded`=?, received=NOW() WHERE `unique`=?";
    /** 重置奖励已领取标志 SQL / Reset reward received flag SQL*/
    private static final String UPDATE_QUERY_DOWN = "UPDATE `web_reward` SET `rewarded`=? WHERE `unique`=?";
    /** 查询可用奖励 SQL / Select available rewards SQL*/
    private static final String SELECT_QUERY = "SELECT * FROM `web_reward` WHERE `item_owner`=? AND `rewarded`=?";

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * @param arg0 数据库名 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String arg0, int arg1, int arg2) {
        return DAOUtils.supports(arg0, arg1, arg2);
    }

    /**
     * 将奖励标记为未领取。
     * Marks a reward as not yet received.
     *
     * unique reward id
     */
    public void setUpdateDown(int unique) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY_DOWN)) {

            stmt.setInt(1, 0);
            stmt.setInt(2, unique);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error(I18n.get("log.865c33530603", unique, e), e);
        }
    }

    /**
     * 将奖励标记为已领取。
     * Marks a reward as received.
     *
     * unique reward id
     *
     * @param unique
     * @return 是否更新成功 / whether update succeeded
     */
    public boolean setUpdate(int unique) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, 1);
            stmt.setInt(2, unique);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error(I18n.get("log.248cee6e9874", unique, e), e);
            return false;
        }
    }

    /**
     * 获取玩家尚未领取的 Web 奖励列表。
     * Gets the list of unclaimed web rewards for a player.
     *
     * player id
     *
     * @param playerId
     * @return 可用奖励条目 / available reward entries
     */
    @Override
    public List<RewardEntryItem> getAvailable(int playerId) {
        List<RewardEntryItem> list = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, 0);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int unique = rset.getInt("unique");
                    int item_id = rset.getInt("item_id");
                    long count = rset.getLong("item_count");
                    list.add(new RewardEntryItem(unique, item_id, count));
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.5f1e9111257c", playerId, e), e);
        }
        return list;
    }

    /**
     * 批量将奖励标记为已领取。
     * Batch-marks rewards as received.
     *
     * @param ids 奖励唯一 ID 列表 / list of unique reward ids
     */
    @Override
    public void uncheckAvailable(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            for (int uniqid : ids) {
                stmt.setInt(1, 1);
                stmt.setInt(2, uniqid);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (Exception e) {
            log.error(I18n.get("log.563cfc162266", e), e);
        }
    }
}
