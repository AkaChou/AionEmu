package com.aionemu.loginserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.SvStatsDAO;

/**
 * 服务器在线统计 DAO 的 MySQL 8 实现。
 * MySQL 8 SvStatsDAO implementation (server online statistics).
 *
 * @author Updated for MySQL 8
 */
@Slf4j
public class MySQL8SvStatsDAO extends SvStatsDAO {


    /** 更新在线服务器统计 SQL / Update online server stats SQL */
    private static final String UPDATE_ONLINE = "UPDATE `svstats` SET status = ?, current = ?, max = ?, last_update = NOW() WHERE server = ?";
    /** 更新离线服务器统计 SQL / Update offline server stats SQL */
    private static final String UPDATE_OFFLINE = "UPDATE `svstats` SET status = ?, current = ?, last_update = NOW() WHERE server = ?";
    /** Mark all servers offline SQL / Mark all servers offline SQL */
    private static final String UPDATE_ALL_OFFLINE = "UPDATE `svstats` SET status = ?, current = ?, last_update = NOW()";

    @Override
    public void update_SvStats_Online(int server, int status, int current, int max) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ONLINE)) {

            stmt.setInt(1, status);
            stmt.setInt(2, current);
            stmt.setInt(3, max);
            stmt.setInt(4, server);

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                insertServerStats(server, status, current, max);
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.e8405213aab7", server, e));
        }
    }

    /**
     * 插入新的服务器统计行（更新未命中时调用）。
     * Inserts a new server stats row when update hits zero rows.
     *
     * server id
     * status
     * current online
     * @param max 最大在线 / max online
     */
    private void insertServerStats(int server, int status, int current, int max) {
        String insertQuery = "INSERT INTO svstats (server, status, current, max, last_update) VALUES (?, ?, ?, ?, NOW())";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(insertQuery)) {

            stmt.setInt(1, server);
            stmt.setInt(2, status);
            stmt.setInt(3, current);
            stmt.setInt(4, max);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.ccd3f03c62c4", server, e));
        }
    }

    @Override
    public void update_SvStats_Offline(int server, int status, int current) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_OFFLINE)) {

            stmt.setInt(1, status);
            stmt.setInt(2, current);
            stmt.setInt(3, server);

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                insertServerStats(server, status, current, 0);
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.edce6e5f9a35", server, e));
        }
    }

    @Override
    public void update_SvStats_All_Offline(int status, int current) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ALL_OFFLINE)) {

            stmt.setInt(1, status);
            stmt.setInt(2, current);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.4b7fedeec762", e));
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
