package com.aionemu.loginserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.model.base.BannedMacEntry;

/**
 * MAC 封禁 DAO 的 MySQL 8 实现。
 * MySQL 8 BannedMacDAO implementation.
 *
 * @author Updated for MySQL 8
 */
@Slf4j
public class MySQL8BannedMacDAO extends BannedMacDAO {


    @Override
    public Map<String, BannedMacEntry> load() {
        Map<String, BannedMacEntry> map = new ConcurrentHashMap<>();
        String query = "SELECT `address`, `time`, `details` FROM `banned_mac`";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String address = rs.getString("address");
                map.put(address, new BannedMacEntry(address, rs.getTimestamp("time"), rs.getString("details")));
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.8e046bb74f14", e));
        }

        return map;
    }

    @Override
    public boolean update(BannedMacEntry entry) {
        String query = "REPLACE INTO `banned_mac` (`address`, `time`, `details`) VALUES (?, ?, ?)";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, entry.getMac());
            ps.setTimestamp(2, entry.getTime());
            ps.setString(3, entry.getDetails());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.78eb3d669103", entry.getMac(), e));
        }

        return false;
    }

    @Override
    public boolean remove(String address) {
        String query = "DELETE FROM `banned_mac` WHERE address = ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setString(1, address);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.1f75cfd0c929", address, e));
        }

        return false;
    }

    @Override
    public void cleanExpiredBans() {
        String query = "DELETE FROM `banned_mac` WHERE time < CURDATE()";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                log.info(I18n.get("log.bfee9964538a", deleted));
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.043ac354757c", e));
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
