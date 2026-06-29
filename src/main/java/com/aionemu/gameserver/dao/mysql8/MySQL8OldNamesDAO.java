package com.aionemu.gameserver.dao.mysql8;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.OldNamesDAO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author synchro2
 * Updated for MySQL 8 - Fixed connection leaks
 */
@Slf4j
public class MySQL8OldNamesDAO extends OldNamesDAO {


	private static final String INSERT_QUERY = "INSERT INTO `old_names` (`player_id`, `old_name`, `new_name`) VALUES (?,?,?)";
	private static final String CHECK_QUERY = "SELECT count(player_id) as cnt FROM old_names WHERE ? = old_names.old_name";

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
			log.error("Can't check if name " + name + ", is used, returning positive result", e);
			return true;
		}
	}

	@Override
	public void insertNames(final int id, final String oldname, final String newname) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
			
			stmt.setInt(1, id);
			stmt.setString(2, oldname);
			stmt.setString(3, newname);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error inserting old name record", e);
		}
	}

	@Override
	public boolean supports(String s, int i, int i1) {
		return MySQL8DAOUtils.supports(s, i, i1);
	}
}