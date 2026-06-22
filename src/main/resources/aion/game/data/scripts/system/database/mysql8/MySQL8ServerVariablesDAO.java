package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.ServerVariablesDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Ben
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8ServerVariablesDAO extends ServerVariablesDAO {

	private static Logger log = LoggerFactory.getLogger(ServerVariablesDAO.class);
	
	private static final String SELECT_QUERY = "SELECT `value` FROM `server_variables` WHERE `key`=?";
	private static final String REPLACE_QUERY = "REPLACE INTO `server_variables` (`key`,`value`) VALUES (?,?)";

	@Override
	public int load(String var) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {
			
			ps.setString(1, var);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Integer.parseInt(rs.getString("value"));
				}
			}
		} catch (SQLException e) {
			log.error("Error loading last saved server time", e);
		}
		return 0;
	}

	@Override
	public boolean store(String var, int time) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(REPLACE_QUERY)) {
			
			ps.setString(1, var);
			ps.setString(2, String.valueOf(time));
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("Error storing server time", e);
			return false;
		}
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}