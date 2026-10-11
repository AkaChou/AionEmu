package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 高级点数/Luna 点 DAO 的 MySQL 8 实现。
 * MySQL 8 PremiumDAO implementation (toll and Luna) using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j(topic = "PREMIUM_CTRL")
@Repository
public class PremiumDAO extends com.aionemu.loginserver.dao.PremiumDAO {

    private static final String SELECT_TOLL_QUERY = "SELECT toll FROM account_data WHERE id = ?";
    private static final String SELECT_REWARDS_QUERY = "SELECT uniqId, points FROM account_rewards WHERE accountId = ? AND rewarded = 0";
    private static final String UPDATE_REWARD_QUERY = "UPDATE account_rewards SET rewarded = 1, received = NOW() WHERE uniqId = ?";
    private static final String UPDATE_TOLL_QUERY = "UPDATE account_data SET toll = ? WHERE id = ?";
    private static final String SELECT_LUNA_QUERY = "SELECT luna FROM account_data WHERE id = ?";
    private static final String UPDATE_LUNA_QUERY = "UPDATE account_data SET luna = ? WHERE id = ?";

    private final JdbcClient jdbcClient;

    public PremiumDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PremiumDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public long getPoints(int accountId) {
        long points = 0;
        try {
            Long toll = jdbcClient.sql(SELECT_TOLL_QUERY)
                .param(accountId)
                .query(Long.class)
                .optional()
                .orElse(null);
            if (toll != null) {
                points = toll;
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.3f7cc1bd1041", accountId), e);
        }

        List<Integer> rewarded = new ArrayList<>();
        try {
            List<RewardEntry> list = jdbcClient.sql(SELECT_REWARDS_QUERY)
                .param(accountId)
                .query((rs, _) -> new RewardEntry(rs.getInt("uniqId"), rs.getLong("points")))
                .list();
            for (RewardEntry entry : list) {
                points += entry.points();
                log.info(I18n.get("log.e7ab6f9f7f1a", accountId, entry.uniqId()));
                rewarded.add(entry.uniqId());
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.a296b4ad2c95", accountId), e);
        }

        if (!rewarded.isEmpty()) {
            try {
                for (int uniqId : rewarded) {
                    jdbcClient.sql(UPDATE_REWARD_QUERY)
                        .param(uniqId)
                        .update();
                }
            } catch (DataAccessException e) {
                log.error(I18n.get("log.25ee99257715", accountId), e);
            }
        }

        return points;
    }

    private record RewardEntry(int uniqId, long points) {}

    @Override
    public boolean updatePoints(int accountId, long points, long required) {
        try {
            int affected = jdbcClient.sql(UPDATE_TOLL_QUERY)
                .param(points - required)
                .param(accountId)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.62998c0631eb", accountId), e);
            return false;
        }
    }

    @Override
    public long getLuna(int accountId) {
        try {
            return jdbcClient.sql(SELECT_LUNA_QUERY)
                .param(accountId)
                .query(Long.class)
                .optional()
                .orElse(0L);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.d397b8bcef72", accountId), e);
            return 0L;
        }
    }

    @Override
    public void updateLuna(int accountId, long luna) {
        try {
            jdbcClient.sql(UPDATE_LUNA_QUERY)
                .param(luna)
                .param(accountId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.83d2610955c2", accountId), e);
            return;
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
