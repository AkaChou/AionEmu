package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.BlockedPlayer;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ben
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8BlockListDAO extends BlockListDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8BlockListDAO.class);
	
	private static final String LOAD_QUERY = "SELECT blocked_player, reason FROM blocks WHERE player=?";
	private static final String ADD_QUERY = "INSERT INTO blocks (player, blocked_player, reason) VALUES (?, ?, ?)";
	private static final String DEL_QUERY = "DELETE FROM blocks WHERE player=? AND blocked_player=?";
	private static final String SET_REASON_QUERY = "UPDATE blocks SET reason=? WHERE player=? AND blocked_player=?";

	@Override
	public boolean addBlockedUser(final int playerObjId, final int objIdToBlock, final String reason) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(ADD_QUERY)) {
			
			stmt.setInt(1, playerObjId);
			stmt.setInt(2, objIdToBlock);
			stmt.setString(3, reason);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("Error adding blocked user - player: {} blocked: {}", playerObjId, objIdToBlock, e);
			return false;
		}
	}

	@Override
	public boolean delBlockedUser(final int playerObjId, final int objIdToDelete) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DEL_QUERY)) {
			
			stmt.setInt(1, playerObjId);
			stmt.setInt(2, objIdToDelete);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("Error deleting blocked user - player: {} blocked: {}", playerObjId, objIdToDelete, e);
			return false;
		}
	}

	@Override
	public BlockList load(final Player player) {
		final Map<Integer, BlockedPlayer> list = new HashMap<Integer, BlockedPlayer>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(LOAD_QUERY)) {
			
			stmt.setInt(1, player.getObjectId());
			
			try (ResultSet rset = stmt.executeQuery()) {
				PlayerDAO playerDao = DAOManager.getDAO(PlayerDAO.class);
				while (rset.next()) {
					int blockedOid = rset.getInt("blocked_player");
					PlayerCommonData pcd = playerDao.loadPlayerCommonData(blockedOid);
					if (pcd == null) {
						log.error("Attempt to load block list for " + player.getName() + " tried to load a player which does not exist: " + blockedOid);
					} else {
						list.put(blockedOid, new BlockedPlayer(pcd, rset.getString("reason")));
					}
				}
			}
		} catch (Exception e) {
			log.error("Could not restore BlockList data for player: " + player.getObjectId(), e);
		}
		return new BlockList(list);
	}

	@Override
	public boolean setReason(final int playerObjId, final int blockedPlayerObjId, final String reason) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SET_REASON_QUERY)) {
			
			stmt.setString(1, reason);
			stmt.setInt(2, playerObjId);
			stmt.setInt(3, blockedPlayerObjId);
			return stmt.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("Error setting block reason - player: {} blocked: {}", playerObjId, blockedPlayerObjId, e);
			return false;
		}
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}