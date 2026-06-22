package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerCreativityPointsDAO;
import com.aionemu.gameserver.model.cp.PlayerCPEntry;
import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerCreativityPointsDAO extends PlayerCreativityPointsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerCreativityPointsDAO.class);

	private static final String INSERT_OR_UPDATE = "INSERT INTO `player_cp` (`player_id`, `slot`, `point`) VALUES(?,?,?) " + "ON DUPLICATE KEY UPDATE `slot` = VALUES(`slot`), `point` = VALUES(`point`)";
	private static final String SELECT_QUERY = "SELECT `slot`,`point` FROM `player_cp` WHERE `player_id`=?";
	private static final String DELETE_QUERY = "DELETE FROM `player_cp` WHERE `player_id`=? AND `slot`=?";
	private static final String SELECT_COUNT_QUERY = "SELECT COUNT(*) AS `size` FROM `player_cp` WHERE `player_id`=?";
	private static final String SELECT_SLOT_QUERY = "SELECT `slot` FROM `player_cp` WHERE `player_id`=?";

	@Override
	public PlayerCPList loadCP(Player player) {
		List<PlayerCPEntry> cp = new ArrayList<PlayerCPEntry>();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			
			stmt.setInt(1, player.getObjectId());
			
			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int slot = rset.getInt("slot");
					int point = rset.getInt("point");
					cp.add(new PlayerCPEntry(slot, point, PersistentState.UPDATED));
				}
			}
		} catch (Exception e) {
			log.error("Could not restore CP time for playerObjId: " + player.getObjectId() + " from DB", e);
		}
		return new PlayerCPList(cp);
	}

	@Override
	public boolean storeCP(int objectId, int slot, int point) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_OR_UPDATE)) {
			
			stmt.setInt(1, objectId);
			stmt.setInt(2, slot);
			stmt.setInt(3, point);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error("Could not store CP for player " + objectId + " from DB", e);
			return false;
		}
	}

	@Override
	public boolean deleteCP(int objectId, int slot) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
			
			stmt.setInt(1, objectId);
			stmt.setInt(2, slot);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error("Could not delete CP for player " + objectId + " from DB", e);
			return false;
		}
	}

	@Override
	public int getSlotSize(int playerObjId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_COUNT_QUERY)) {
			
			stmt.setInt(1, playerObjId);
			
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("size");
				}
			}
		} catch (Exception e) {
			log.error("Error getting slot size for player: {}", playerObjId, e);
		}
		return 0;
	}

	@Override
	public int getCPSlotObjId(final int obj) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement s = con.prepareStatement(SELECT_SLOT_QUERY)) {
			
			s.setInt(1, obj);
			
			try (ResultSet rs = s.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("slot");
				}
			}
		} catch (Exception e) {
			log.error("Error getting CP slot for player: {}", obj, e);
		}
		return 0;
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}