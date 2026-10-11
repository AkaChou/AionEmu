package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.model.BannedIP;

import lombok.extern.slf4j.Slf4j;

/**
 * IP 封禁 DAO 的 MySQL 8 实现。
 * MySQL 8 BannedIpDAO implementation using JdbcClient.
 */
@Slf4j
@Repository
public class BannedIpDAO extends com.aionemu.loginserver.dao.BannedIpDAO {

    private static final String INSERT_QUERY = "INSERT INTO banned_ip(mask, time_end) VALUES (?, ?)";
    private static final String UPDATE_QUERY = "UPDATE banned_ip SET mask = ?, time_end = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM banned_ip WHERE mask = ?";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM banned_ip ORDER BY id";
    private static final String CLEAN_EXPIRED_QUERY = "DELETE FROM banned_ip WHERE time_end < CURRENT_TIMESTAMP AND time_end IS NOT NULL";

    private final JdbcClient jdbcClient;

    public BannedIpDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public BannedIpDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public BannedIP insert(String mask) {
        return insert(mask, null);
    }

    @Override
    public BannedIP insert(final String mask, final Timestamp expireTime) {
        BannedIP result = new BannedIP();
        result.setMask(mask);
        result.setTimeEnd(expireTime);

        return insert(result) ? result : null;
    }

    @Override
    public boolean insert(final BannedIP bannedIP) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int affected = jdbcClient.sql(INSERT_QUERY)
                .param(bannedIP.getMask())
                .param(bannedIP.getTimeEnd())
                .update(keyHolder, "id");

            if (affected > 0 && keyHolder.getKey() != null) {
                bannedIP.setId(keyHolder.getKey().intValue());
                return true;
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2317a7af1cc6", bannedIP.getMask()), e);
        }
        return false;
    }

    @Override
    public boolean update(final BannedIP bannedIP) {
        try {
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(bannedIP.getMask())
                .param(bannedIP.getTimeEnd())
                .param(bannedIP.getId())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e5c0c472a65c", bannedIP.getId()), e);
            return false;
        }
    }

    @Override
    public boolean remove(final String mask) {
        try {
            int affected = jdbcClient.sql(DELETE_QUERY)
                .param(mask)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.19986598786f", mask), e);
            return false;
        }
    }

    @Override
    public boolean remove(final BannedIP bannedIP) {
        return remove(bannedIP.getMask());
    }

    @Override
    public Set<BannedIP> getAllBans() {
        Set<BannedIP> result = new HashSet<>();
        try {
			result.addAll(jdbcClient.sql(SELECT_ALL_QUERY)
				.query((rs, _) -> {
					BannedIP ip = new BannedIP();
					ip.setId(rs.getInt("id"));
					ip.setMask(rs.getString("mask"));
					ip.setTimeEnd(rs.getTimestamp("time_end"));
					return ip;
				})
				.list());
        } catch (DataAccessException e) {
            log.error(I18n.get("log.5a0812fa8b4c"), e);
        }
        return result;
    }

    @Override
    public void cleanExpiredBans() {
        try {
            int deleted = jdbcClient.sql(CLEAN_EXPIRED_QUERY).update();
            if (deleted > 0) {
                log.info(I18n.get("log.351f92913e18", deleted));
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.42ddf2573c66"), e);
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
