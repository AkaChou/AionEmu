package com.aionemu.loginserver.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.model.Vip;

import lombok.extern.slf4j.Slf4j;

/**
 * MySQL account VIP persistence.
 */
@Slf4j
public class VipDAO extends com.aionemu.loginserver.dao.VipDAO {

    static final String FIND_QUERY = "SELECT account_id, vip_level, vip_exp "
        + "FROM account_vip WHERE account_id = ?";
    static final String SYNC_QUERY = "INSERT IGNORE INTO account_vip (account_id, vip_level) "
        + "SELECT id, ? FROM account_data";
    static final String INSERT_QUERY = "INSERT IGNORE INTO account_vip (account_id, vip_level) VALUES (?, ?)";

    @Override
    public Vip findByAccountId(int accountId) {
        try (Connection connection = DatabaseFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_QUERY)) {
            statement.setInt(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Vip(
                        resultSet.getInt("account_id"),
                        resultSet.getInt("vip_level"),
                        resultSet.getLong("vip_exp")
                    );
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.2a8dc6bf0f91", accountId), e);
        }
        return null;
    }

    @Override
    public int syncMissingAccounts(int level) {
        try (Connection connection = DatabaseFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(SYNC_QUERY)) {
            statement.setInt(1, level);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not synchronize missing account VIP data", e);
        }
    }

    @Override
    public boolean insertIfAbsent(int accountId, int level) {
        try (Connection connection = DatabaseFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_QUERY)) {
            statement.setInt(1, accountId);
            statement.setInt(2, level);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.7bc951a4d602", accountId), e);
            return false;
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
