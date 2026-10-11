package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;

import lombok.extern.slf4j.Slf4j;

/**
 * Web 奖励服务 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of RewardServiceDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
@Repository
public class RewardServiceDAO extends com.aionemu.gameserver.dao.RewardServiceDAO {

    /** 标记奖励为已领取 SQL / Mark reward as received SQL*/
    private static final String UPDATE_QUERY = "UPDATE `web_reward` SET `rewarded`=?, received=NOW() WHERE `unique`=?";
    /** 重置奖励已领取标志 SQL / Reset reward received flag SQL*/
    private static final String UPDATE_QUERY_DOWN = "UPDATE `web_reward` SET `rewarded`=? WHERE `unique`=?";
    /** 查询可用奖励 SQL / Select available rewards SQL*/
    private static final String SELECT_QUERY = "SELECT * FROM `web_reward` WHERE `item_owner`=? AND `rewarded`=?";

    private final JdbcClient jdbcClient;

    public RewardServiceDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public RewardServiceDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     * @param database 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }

    /**
     * 将奖励标记为未领取。
     * Marks a reward as not yet received.
     * @param unique 唯一奖励 ID / unique reward id
     */
    public void setUpdateDown(int unique) {
        try {
            jdbcClient.sql(UPDATE_QUERY_DOWN)
                .param(0)
                .param(unique)
                .update();
        } catch (Exception e) {
            log.error(I18n.get("log.865c33530603", unique), e);
        }
    }

    /**
     * 将奖励标记为已领取。
     * Marks a reward as received.
     * @param unique 唯一奖励 ID / unique reward id
     * @return 是否更新成功 / whether update succeeded
     */
    public boolean setUpdate(int unique) {
        try {
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(1)
                .param(unique)
                .update();
            return affected > 0;
        } catch (Exception e) {
            log.error(I18n.get("log.248cee6e9874", unique), e);
            return false;
        }
    }

    /**
     * 获取玩家尚未领取的 Web 奖励列表。
     * Gets the list of unclaimed web rewards for a player.
     * @param playerId 玩家 ID / player id
     * @return 可用奖励条目 / available reward entries
     */
    @Override
    public List<RewardEntryItem> getAvailable(int playerId) {
        List<RewardEntryItem> list = new ArrayList<>();
        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(playerId)
                .param(0)
                .query((rs, _) -> {
                    int unique = rs.getInt("unique");
                    int item_id = rs.getInt("item_id");
                    long count = rs.getLong("item_count");
                    return new RewardEntryItem(unique, item_id, count);
                })
                .list()
                .forEach(list::add);
        } catch (Exception e) {
            log.error(I18n.get("log.5f1e9111257c", playerId), e);
        }
        return list;
    }

    /**
     * 批量将奖励标记为已领取。
     * Batch-marks rewards as received.
     * @param ids 奖励唯一 ID 列表 / list of unique reward ids
     */
    @Override
    public void uncheckAvailable(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        try {
            for (int uniqid : ids) {
                jdbcClient.sql(UPDATE_QUERY)
                    .param(1)
                    .param(uniqid)
                    .update();
            }
        } catch (Exception e) {
            log.error(I18n.get("log.563cfc162266"), e);
        }
    }
}
