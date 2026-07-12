package com.aionemu.loginserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.GameServerInfo;

/**
 * 游戏服务器列表 DAO 的 MySQL 8 实现。
 * MySQL 8 GameServersDAO implementation.
 *
 * @author Updated for MySQL 8
 */
@Slf4j
public class GameServersDAO extends com.aionemu.loginserver.dao.GameServersDAO {


    @Override
    public Map<Byte, GameServerInfo> getAllGameServers() {
        Map<Byte, GameServerInfo> result = new HashMap<>();
        String query = "SELECT * FROM gameservers ORDER BY id";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query);
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                byte id = rs.getByte("id");
                String ipMask = rs.getString("mask");
                String password = rs.getString("password");
                GameServerInfo gsi = new GameServerInfo(id, ipMask, password);
                result.put(id, gsi);
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.cb987d22f8fa", e));
        }

        return result;
    }

    @Override
    public boolean supports(String s, int i, int i1) {
        return DAOUtils.supports(s, i, i1);
    }
}
