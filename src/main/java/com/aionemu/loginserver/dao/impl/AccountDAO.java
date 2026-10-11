package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.model.Account;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号 DAO 的 MySQL 8 实现。
 * MySQL 8 AccountDAO implementation using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j
@Repository
public class AccountDAO extends com.aionemu.loginserver.dao.AccountDAO {

    private static final String SELECT_BY_NAME_QUERY = "SELECT * FROM account_data WHERE `name` = ?";
    private static final String SELECT_BY_ID_QUERY = "SELECT * FROM account_data WHERE `id` = ?";
    private static final String SELECT_ID_BY_NAME_QUERY = "SELECT `id` FROM account_data WHERE `name` = ?";
    private static final String SELECT_COUNT_QUERY = "SELECT COUNT(*) AS c FROM account_data";
    private static final String INSERT_ACCOUNT_QUERY = "INSERT INTO account_data(`name`, `password`, access_level, membership, activated, "
        + "last_server, last_ip, last_mac, ip_force, toll) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_ACCOUNT_QUERY = "UPDATE account_data SET `name` = ?, `password` = ?, access_level = ?, membership = ?, "
        + "activated = ?, last_server = ?, last_ip = ?, last_mac = ?, ip_force = ?, return_account = ?, "
        + "return_end = ? WHERE `id` = ?";
    private static final String UPDATE_LAST_SERVER_QUERY = "UPDATE account_data SET last_server = ? WHERE id = ?";
    private static final String UPDATE_LAST_IP_QUERY = "UPDATE account_data SET last_ip = ? WHERE id = ?";
    private static final String SELECT_LAST_IP_QUERY = "SELECT `last_ip` FROM `account_data` WHERE `id` = ?";
    private static final String UPDATE_LAST_MAC_QUERY = "UPDATE `account_data` SET `last_mac` = ? WHERE `id` = ?";
    private static final String UPDATE_MEMBERSHIP_QUERY = "UPDATE account_data SET membership = old_membership, expire = NULL "
        + "WHERE id = ? AND expire < CURRENT_TIMESTAMP";
    private static final String DELETE_INACTIVE_ACCOUNTS_QUERY = "DELETE FROM account_data WHERE id IN ("
        + "SELECT account_id FROM account_time WHERE DATEDIFF(CURDATE(), last_active) > ?)";

    private final JdbcClient jdbcClient;

    public AccountDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public AccountDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public Account getAccount(String name) {
        try {
            return jdbcClient.sql(SELECT_BY_NAME_QUERY)
                .param(name)
                .query((rs, _) -> mapAccount(rs))
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7be9f6648605", name), e);
            return null;
        }
    }

    @Override
    public Account getAccount(int id) {
        try {
            return jdbcClient.sql(SELECT_BY_ID_QUERY)
                .param(id)
                .query((rs, _) -> mapAccount(rs))
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.443dba3838a7", id), e);
            return null;
        }
    }

    private static Account mapAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getInt("id"));
        account.setName(rs.getString("name"));
        account.setPasswordHash(rs.getString("password"));
        account.setAccessLevel(rs.getByte("access_level"));
        account.setMembership(rs.getByte("membership"));
        account.setActivated(rs.getByte("activated"));
        account.setLastServer(rs.getByte("last_server"));
        account.setLastIp(rs.getString("last_ip"));
        account.setLastMac(rs.getString("last_mac"));
        account.setIpForce(rs.getString("ip_force"));
        account.setReturn(rs.getByte("return_account"));
        account.setReturnEnd(rs.getTimestamp("return_end"));
        return account;
    }

    @Override
    public boolean insertAccount(Account account) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int result = jdbcClient.sql(INSERT_ACCOUNT_QUERY)
                .param(account.getName())
                .param(account.getPasswordHash())
                .param(account.getAccessLevel())
                .param(account.getMembership())
                .param(account.getActivated())
                .param(account.getLastServer())
                .param(account.getLastIp())
                .param(account.getLastMac())
                .param(account.getIpForce())
                .param(0L)
                .update(keyHolder, "id");

            if (result > 0) {
                if (keyHolder.getKey() != null) {
                    account.setId(keyHolder.getKey().intValue());
                }
                return true;
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.34a80423f0dc"), e);
        }
        return false;
    }

    @Override
    public boolean updateAccount(Account account) {
        try {
            int affected = jdbcClient.sql(UPDATE_ACCOUNT_QUERY)
                .param(account.getName())
                .param(account.getPasswordHash())
                .param(account.getAccessLevel())
                .param(account.getMembership())
                .param(account.getActivated())
                .param(account.getLastServer())
                .param(account.getLastIp())
                .param(account.getLastMac())
                .param(account.getIpForce())
                .param(account.getReturn())
                .param(account.getReturnEnd())
                .param(account.getId())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.40f6bd6c99c5", account.getId()), e);
            return false;
        }
    }

    @Override
    public void updateInTransaction(Connection con, Account account) throws SQLException {
        try (PreparedStatement st = con.prepareStatement(UPDATE_ACCOUNT_QUERY)) {
            st.setString(1, account.getName());
            st.setString(2, account.getPasswordHash());
            st.setByte(3, account.getAccessLevel());
            st.setByte(4, account.getMembership());
            st.setByte(5, account.getActivated());
            st.setByte(6, account.getLastServer());
            st.setString(7, account.getLastIp());
            st.setString(8, account.getLastMac());
            st.setString(9, account.getIpForce());
            st.setByte(10, account.getReturn());
            st.setTimestamp(11, account.getReturnEnd());
            st.setInt(12, account.getId());
            if (st.executeUpdate() == 0) {
                throw new SQLException("No account row changed for " + account.getId());
            }
        }
    }

    @Override
    public void updateLastServer(final int accountId, final byte lastServer) {
        try {
            jdbcClient.sql(UPDATE_LAST_SERVER_QUERY)
                .param(lastServer)
                .param(accountId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7a357f84a3ac", accountId), e);
        }
    }

    @Override
    public void updateLastIp(final int accountId, final String ip) {
        try {
            jdbcClient.sql(UPDATE_LAST_IP_QUERY)
                .param(ip)
                .param(accountId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.a2c8c099cda2", accountId), e);
        }
    }

    @Override
    public String getLastIp(final int accountId) {
        try {
            String ip = jdbcClient.sql(SELECT_LAST_IP_QUERY)
                .param(accountId)
                .query(String.class)
                .optional()
                .orElse(null);
            return ip != null ? ip : "";
        } catch (DataAccessException e) {
            log.error(I18n.get("log.6acadf7febd4", accountId), e);
            return "";
        }
    }

    @Override
    public boolean updateLastMac(final int accountId, final String mac) {
        try {
            int affected = jdbcClient.sql(UPDATE_LAST_MAC_QUERY)
                .param(mac)
                .param(accountId)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8cb7e88bf3fb", accountId), e);
            return false;
        }
    }

    @Override
    public boolean updateMembership(final int accountId) {
        try {
            int affected = jdbcClient.sql(UPDATE_MEMBERSHIP_QUERY)
                .param(accountId)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.971689f314b7", accountId), e);
            return false;
        }
    }

    @Override
    public void deleteInactiveAccounts(int daysOfInactivity) {
        try {
            jdbcClient.sql(DELETE_INACTIVE_ACCOUNTS_QUERY)
                .param(daysOfInactivity)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.13feade4c9b9"), e);
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
