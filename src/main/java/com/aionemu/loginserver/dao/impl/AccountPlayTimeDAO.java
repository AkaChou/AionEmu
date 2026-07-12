package com.aionemu.loginserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.model.AccountTime;

/**
 * 账号累计在线时长 DAO 的 MySQL 8 实现。
 * MySQL 8 AccountPlayTimeDAO implementation.
 *
 * @author Updated for MySQL 8
 */
@Slf4j
public class AccountPlayTimeDAO extends com.aionemu.loginserver.dao.AccountPlayTimeDAO {


    @Override
    public boolean update(final Integer accountId, final AccountTime accountTime) {
        String query = "INSERT INTO account_playtime (`account_id`, `accumulated_online`) VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE `accumulated_online` = `accumulated_online` + ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {

            st.setInt(1, accountId);
            st.setLong(2, accountTime.getAccumulatedOnlineTime());
            st.setLong(3, accountTime.getAccumulatedOnlineTime());

            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.f2c20e9730e3", accountId, e));
        }

        return false;
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
