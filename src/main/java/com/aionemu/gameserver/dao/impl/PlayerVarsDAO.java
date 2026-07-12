package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import java.util.LinkedHashMap;
import java.util.Map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
/**
 * 玩家变量 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerVarsDAO.
 *
 * @author KID
 */
@Slf4j
public class PlayerVarsDAO extends com.aionemu.gameserver.dao.PlayerVarsDAO {


    /** 查询变量 SQL / Select variables SQL*/
    private static final String SELECT_QUERY = "SELECT param,value FROM player_vars WHERE player_id=?";
    /** 插入变量 SQL / Insert variable SQL*/
    private static final String INSERT_QUERY = "INSERT INTO player_vars (`player_id`, `param`, `value`, `time`) VALUES (?,?,?,NOW())";
    /** 删除变量 SQL / Delete variable SQL*/
    private static final String DELETE_QUERY = "DELETE FROM player_vars WHERE player_id=? AND param=?";

    /**
     * 加载玩家全部自定义变量。
     * Loads all custom variables for a player.
     *
     * player id
     * variable map
     */
    @Override
    public Map<String, Object> load(final int playerId) {
        final Map<String, Object> map = new LinkedHashMap<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(SELECT_QUERY)) {

            st.setInt(1, playerId);

            try (ResultSet rset = st.executeQuery()) {
                while (rset.next()) {
                    String key = rset.getString("param");
                    String value = rset.getString("value");
                    map.put(key, value);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.eef4c3c016c8", playerId, e));
        }
        return map;
    }

    /**
     * 设置玩家变量。
     * Sets a player variable.
     *
     * player id
     * variable key
     * variable value
     *
     * @return 是否设置成功 / whether the set succeeded
     */
    @Override
    public boolean set(final int playerId, final String key, final Object value) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setString(2, key);
            stmt.setString(3, value.toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.093f2900065e", playerId, key, e));
            return false;
        }
    }

    /**
     * 删除玩家变量。
     * Removes a player variable.
     *
     * player id
     * variable key
     *
     * @return 是否删除成功 / whether the remove succeeded
     */
    @Override
    public boolean remove(final int playerId, final String key) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setString(2, key);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.c3f19e683101", playerId, key, e));
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * @param s 数据库名 / database name
     * @param i 主版本 / major version
     * @param i1 次版本 / minor version
     * whether supported
     */
    @Override
    public boolean supports(String s, int i, int i1) {
        return DAOUtils.supports(s, i, i1);
    }
}
