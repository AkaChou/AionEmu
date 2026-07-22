package com.aionemu.loginserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;

/**
 * 玩家转服任务 DAO 的 MySQL 8 实现。
 * MySQL 8 PlayerTransferDAO implementation.
 *
 * @author Updated for MySQL 8
 */
@Slf4j
public class PlayerTransferDAO extends com.aionemu.loginserver.dao.PlayerTransferDAO {


    @Override
    public List<PlayerTransferTask> getNew() {
        List<PlayerTransferTask> list = new ArrayList<>();
        String query = "SELECT * FROM player_transfers WHERE `status` = ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {

            st.setInt(1, 0);

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    PlayerTransferTask task = new PlayerTransferTask();
                    task.id = rs.getInt("id");
                    task.sourceServerId = (byte) rs.getShort("source_server");
                    task.targetServerId = (byte) rs.getShort("target_server");
                    task.sourceAccountId = rs.getInt("source_account_id");
                    task.targetAccountId = rs.getInt("target_account_id");
                    task.playerId = rs.getInt("player_id");
                    list.add(task);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.ced4ca54368a", e), e);
        }

        return list;
    }

    @Override
    public boolean update(final PlayerTransferTask task) {
        try (Connection con = DatabaseFactory.getConnection()) {
            updateInTransaction(con, task);
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.84835322c373", task.id, e), e);
            return false;
        }
    }

    @Override
    public void updateInTransaction(Connection con, final PlayerTransferTask task) throws SQLException {
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

        try (PreparedStatement st = con.prepareStatement(query.toString())) {

            st.setByte(1, task.status);
            st.setString(2, task.comment);
            st.setInt(3, task.id);

            if (st.executeUpdate() == 0) {
                throw new SQLException("No player transfer row changed for " + task.id);
            }
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
