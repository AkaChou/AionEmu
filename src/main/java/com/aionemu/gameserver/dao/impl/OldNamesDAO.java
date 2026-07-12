package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 旧角色名 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of OldNamesDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author synchro2
 */
@Slf4j
public class OldNamesDAO extends com.aionemu.gameserver.dao.OldNamesDAO {


	/** 插入旧名称 SQL / Insert old name SQL*/
	private static final String INSERT_QUERY = "INSERT INTO `old_names` (`player_id`, `old_name`, `new_name`) VALUES (?,?,?)";
	/** 检查旧名称是否存在 SQL / Check if old name exists SQL */
	private static final String CHECK_QUERY = "SELECT count(player_id) as cnt FROM old_names WHERE ? = old_names.old_name";

	/**
	 * 判断名称是否曾被使用过。
	 * Checks whether the name has been used before.
	 *
	 * character name
	 *
	 * @param name
	 * @return 是否为旧名 / whether it is an old name
	 */
	@Override
	public boolean isOldName(final String name) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement s = con.prepareStatement(CHECK_QUERY)) {

			s.setString(1, name);
			try (ResultSet rs = s.executeQuery()) {
				rs.next();
				return rs.getInt("cnt") > 0;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.b006f4717fd6", name, ", is used, returning positive result", e));
			return true;
		}
	}

	/**
	 * 记录角色改名历史。
	 * Records a character rename history entry.
	 *
	 * @param id 玩家 ID / player id
	 * old name
	 * new name
	 */
	@Override
	public void insertNames(final int id, final String oldname, final String newname) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

			stmt.setInt(1, id);
			stmt.setString(2, oldname);
			stmt.setString(3, newname);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.0f01a0baefea", e));
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
