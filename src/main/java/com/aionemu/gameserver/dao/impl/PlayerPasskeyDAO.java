package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import java.sql.*;

/**
 * 玩家二级密码 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerPasskeyDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class PlayerPasskeyDAO extends com.aionemu.gameserver.dao.PlayerPasskeyDAO {

    /** 插入二级密码 SQL / Insert passkey SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `player_passkey` (`account_id`, `passkey`) VALUES (?,?)";
    /** 更新验证旧密码后的二级密码 SQL / Update passkey after verifying old one SQL*/
    private static final String UPDATE_QUERY = "UPDATE `player_passkey` SET `passkey`=? WHERE `account_id`=? AND `passkey`=?";
    /** 强制更新二级密码 SQL / Force-update passkey SQL*/
    private static final String UPDATE_FORCE_QUERY = "UPDATE `player_passkey` SET `passkey`=? WHERE `account_id`=?";
    /** 检查二级密码 SQL / Check passkey SQL*/
    private static final String CHECK_QUERY = "SELECT COUNT(*) as cnt FROM `player_passkey` WHERE `account_id`=? AND `passkey`=?";
    /** 检查二级密码是否存在 SQL / Check whether passkey exists SQL*/
    private static final String EXIST_CHECK_QUERY = "SELECT COUNT(*) as cnt FROM `player_passkey` WHERE `account_id`=?";

    /**
     * 为账号插入二级密码。
     * Inserts a passkey for an account.
     *
     * @param accountId 账号 ID / account id
     * @param passkey 通行密钥 / passkey
     */
    @Override
    public void insertPlayerPasskey(int accountId, String passkey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setString(2, passkey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.cf0e27a382ab", accountId, e));
        }
    }

    /**
     * 在校验旧密码后更新二级密码。
     * Updates the passkey after verifying the old one.
     *
     * @param accountId 账号 ID / account id
     * @param oldPasskey 旧通行密钥 / old passkey
     * @param newPasskey 新通行密钥 / new passkey
     *
     * @return 是否更新成功 / whether update succeeded
     */
    @Override
    public boolean updatePlayerPasskey(int accountId, String oldPasskey, String newPasskey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setString(1, newPasskey);
            stmt.setInt(2, accountId);
            stmt.setString(3, oldPasskey);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.9cef2da6c580", accountId, e));
            return false;
        }
    }

    /**
     * 强制更新二级密码（不校验旧密码）。
     * Force-updates the passkey without verifying the old one.
     *
     * @param accountId 账号 ID / account id
     * @param newPasskey 新通行密钥 / new passkey
     *
     * @return 是否更新成功 / whether update succeeded
     */
    @Override
    public boolean updateForcePlayerPasskey(int accountId, String newPasskey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_FORCE_QUERY)) {

            stmt.setString(1, newPasskey);
            stmt.setInt(2, accountId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.9cef2da6c580", accountId, e));
            return false;
        }
    }

    /**
     * 校验账号二级密码是否匹配。
     * Checks whether the account passkey matches.
     *
     * @param accountId 账号 ID / account id
     * @param passkey 通行密钥 / passkey
     * @return 是否匹配 / whether it matches
     */
    @Override
    public boolean checkPlayerPasskey(int accountId, String passkey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CHECK_QUERY)) {

            stmt.setInt(1, accountId);
            stmt.setString(2, passkey);

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("cnt") == 1;
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.c22df4b9c35d", accountId, e));
        }
        return false;
    }

    /**
     * 检查账号是否已设置二级密码。
     * Checks whether the account already has a passkey.
     *
     * @param accountId 账号 ID / account id
     * @return 是否已设置 / whether a passkey exists
     */
    @Override
    public boolean existCheckPlayerPasskey(int accountId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(EXIST_CHECK_QUERY)) {

            stmt.setInt(1, accountId);

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("cnt") == 1;
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.c22df4b9c35d", accountId, e));
        }
        return false;
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
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
