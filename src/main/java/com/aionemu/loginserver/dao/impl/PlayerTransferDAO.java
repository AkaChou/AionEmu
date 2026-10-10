package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家转服任务 DAO 的 MySQL 8 实现。
 * MySQL 8 PlayerTransferDAO implementation using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j
@Repository
public class PlayerTransferDAO extends com.aionemu.loginserver.dao.PlayerTransferDAO {

    private static final String SELECT_NEW_QUERY = "SELECT * FROM player_transfers WHERE `status` = ?";

    private final JdbcClient jdbcClient;

    public PlayerTransferDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerTransferDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public List<PlayerTransferTask> getNew() {
        List<PlayerTransferTask> list = new ArrayList<>();
        try {
            return jdbcClient.sql(SELECT_NEW_QUERY)
                .param(0)
                .query((rs, rowNum) -> {
                    PlayerTransferTask task = new PlayerTransferTask();
                    task.id = rs.getInt("id");
                    task.sourceServerId = (byte) rs.getShort("source_server");
                    task.targetServerId = (byte) rs.getShort("target_server");
                    task.sourceAccountId = rs.getInt("source_account_id");
                    task.targetAccountId = rs.getInt("target_account_id");
                    task.playerId = rs.getInt("player_id");
                    return task;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ced4ca54368a"), e);
            return list;
        }
    }

    @Override
    public boolean update(final PlayerTransferTask task) {
        String query = buildUpdateQuery(task);
        try {
            int updated = jdbcClient.sql(query)
                .param(task.status)
                .param(task.comment)
                .param(task.id)
                .update();
            return updated > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.84835322c373", task.id), e);
            return false;
        }
    }

    @Override
    public void updateInTransaction(Connection con, final PlayerTransferTask task) throws SQLException {
        String query = buildUpdateQuery(task);
        try (PreparedStatement st = con.prepareStatement(query)) {
            st.setByte(1, task.status);
            st.setString(2, task.comment);
            st.setInt(3, task.id);

            if (st.executeUpdate() == 0) {
                throw new SQLException("No player transfer row changed for " + task.id);
            }
        }
    }

    private static String buildUpdateQuery(final PlayerTransferTask task) {
        StringBuilder query = new StringBuilder("UPDATE player_transfers SET status = ?, comment = ?");
        switch (task.status) {
            case PlayerTransferTask.STATUS_ACTIVE:
                query.append(", time_performed = NOW()");
                break;
            case PlayerTransferTask.STATUS_DONE:
            case PlayerTransferTask.STATUS_ERROR:
                query.append(", time_done = NOW()");
                break;
        }
        query.append(" WHERE id = ?");
        return query.toString();
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
