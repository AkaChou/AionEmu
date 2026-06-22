package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.AnnouncementsDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.Announcement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Divinity
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8AnnouncementsDAO extends AnnouncementsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8AnnouncementsDAO.class);
	
	private static final String SELECT_QUERY = "SELECT * FROM announcements ORDER BY id";
	private static final String INSERT_QUERY = "INSERT INTO announcements (announce, faction, type, delay) VALUES (?, ?, ?, ?)";
	private static final String DELETE_QUERY = "DELETE FROM announcements WHERE id = ?";

	@Override
	public Set<Announcement> getAnnouncements() {
		final Set<Announcement> result = new HashSet<Announcement>();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
			 ResultSet resultSet = stmt.executeQuery()) {
			
			while (resultSet.next()) {
				result.add(new Announcement(
					resultSet.getInt("id"),
					resultSet.getString("announce"),
					resultSet.getString("faction"),
					resultSet.getString("type"),
					resultSet.getInt("delay")
				));
			}
		} catch (SQLException e) {
			log.error("Error loading announcements", e);
		}
		return result;
	}

	@Override
	public void addAnnouncement(final Announcement announce) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
			
			stmt.setString(1, announce.getAnnounce());
			stmt.setString(2, announce.getFaction());
			stmt.setString(3, announce.getType());
			stmt.setInt(4, announce.getDelay());
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error adding announcement", e);
		}
	}

	@Override
	public boolean delAnnouncement(final int idAnnounce) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
			
			stmt.setInt(1, idAnnounce);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("Error deleting announcement: {}", idAnnounce, e);
			return false;
		}
	}

	@Override
	public boolean supports(String s, int i, int i1) {
		return MySQL8DAOUtils.supports(s, i, i1);
	}
}