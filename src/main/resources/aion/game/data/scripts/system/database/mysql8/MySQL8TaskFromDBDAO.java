package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.TaskFromDBDAO;
import com.aionemu.gameserver.model.tasks.TaskFromDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;

/**
 * @author Divinity
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8TaskFromDBDAO extends TaskFromDBDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8TaskFromDBDAO.class);
	
	private static final String SELECT_ALL_QUERY = "SELECT * FROM tasks ORDER BY id";
	private static final String UPDATE_QUERY = "UPDATE tasks SET last_activation = ? WHERE id = ?";

	@Override
	public ArrayList<TaskFromDB> getAllTasks() {
		final ArrayList<TaskFromDB> result = new ArrayList<TaskFromDB>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_ALL_QUERY);
			 ResultSet rset = stmt.executeQuery()) {
			
			while (rset.next()) {
				result.add(new TaskFromDB(
					rset.getInt("id"),
					rset.getString("task"),
					rset.getString("type"),
					rset.getTimestamp("last_activation"),
					rset.getString("start_time"),
					rset.getInt("delay"),
					rset.getString("param")
				));
            }
		} catch (SQLException e) {
			log.error("getAllTasks", e);
		}
		return result;
	}

	@Override
	public void setLastActivation(final int id) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
			
			stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			stmt.setInt(2, id);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("setLastActivation", e);
		}
	}

	@Override
	public boolean supports(String s, int i, int i1) {
		return MySQL8DAOUtils.supports(s, i, i1);
	}
}