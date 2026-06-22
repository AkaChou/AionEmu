package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.guide.Guide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xTz
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8GuideDAO extends GuideDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8GuideDAO.class);
	
	private static final String DELETE_QUERY = "DELETE FROM `guides` WHERE `guide_id`=?";
	private static final String SELECT_QUERY = "SELECT * FROM `guides` WHERE `player_id`=?";
	private static final String SELECT_GUIDE_QUERY = "SELECT * FROM `guides` WHERE `guide_id`=? AND `player_id`=?";
	private static final String INSERT_QUERY = "INSERT INTO guides(guide_id, title, player_id) VALUES (?, ?, ?)";
	private static final String SELECT_USED_IDS_QUERY = "SELECT guide_id FROM guides";

	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return MySQL8DAOUtils.supports(arg0, arg1, arg2);
	}

	@Override
	public boolean deleteGuide(int guide_id) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
			
			stmt.setInt(1, guide_id);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error("Error delete guide_id: " + guide_id, e);
			return false;
		}
	}

	@Override
	public List<Guide> loadGuides(int playerId) {
		final List<Guide> guides = new ArrayList<Guide>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			
			stmt.setInt(1, playerId);
			
			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int guide_id = rset.getInt("guide_id");
					int player_id = rset.getInt("player_id");
					String title = rset.getString("title");
					Guide guide = new Guide(guide_id, player_id, title);
					guides.add(guide);
				}
			}
		} catch (Exception e) {
			log.error("Could not restore Guide data for player: " + playerId + " from DB", e);
		}
		return guides;
	}

	@Override
	public Guide loadGuide(int player_id, int guide_id) {
		Guide guide = null;
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_GUIDE_QUERY)) {
			
			stmt.setInt(1, guide_id);
			stmt.setInt(2, player_id);

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					String title = rset.getString("title");
					guide = new Guide(guide_id, player_id, title);
				}
			}
		} catch (Exception e) {
			log.error("Could not restore Survey data for player: " + player_id + " from DB", e);
		}
		return guide;
	}

	@Override
	public void saveGuide(int guide_id, Player player, String title) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
			
			stmt.setInt(1, guide_id);
			stmt.setString(2, title);
			stmt.setInt(3, player.getObjectId());
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error("Error saving guide for player: " + player, e);
		}
	}

	@Override
	public int[] getUsedIDs() {
		List<Integer> ids = new ArrayList<Integer>();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement(SELECT_USED_IDS_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			 ResultSet rs = statement.executeQuery()) {
			
			while (rs.next()) {
				ids.add(rs.getInt("guide_id"));
			}
		} catch (SQLException e) {
			log.error("Can't get list of id's from guides table", e);
			return new int[0];
		}
		
		int[] result = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++) {
			result[i] = ids.get(i);
		}
		return result;
	}
}