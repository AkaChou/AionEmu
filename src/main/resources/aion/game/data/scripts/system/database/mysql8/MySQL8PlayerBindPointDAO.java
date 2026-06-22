package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerBindPointDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.BindPointPosition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author evilset
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerBindPointDAO extends PlayerBindPointDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerBindPointDAO.class);

	private static final String INSERT_QUERY = "REPLACE INTO `player_bind_point` (`player_id`, `map_id`, `x`, `y`, `z`, `heading`) VALUES (?,?,?,?,?,?)";
	private static final String SELECT_QUERY = "SELECT `map_id`, `x`, `y`, `z`, `heading` FROM `player_bind_point` WHERE `player_id`=?";
	private static final String UPDATE_QUERY = "UPDATE player_bind_point set `map_id`=?, `x`=?, `y`=?, `z`=?, `heading`=? WHERE `player_id`=?";

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}

	@Override
	public void loadBindPoint(Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			
			stmt.setInt(1, player.getObjectId());
			
			try (ResultSet rset = stmt.executeQuery()) {
				if (rset.next()) {
					int mapId = rset.getInt("map_id");
					float x = rset.getFloat("x");
					float y = rset.getFloat("y");
					float z = rset.getFloat("z");
					byte heading = rset.getByte("heading");
					BindPointPosition bind = new BindPointPosition(mapId, x, y, z, heading);
					bind.setPersistentState(PersistentState.UPDATED);
					player.setBindPoint(bind);
				}
			}
		} catch (Exception e) {
			log.error("Could not restore BindPointPosition data for playerObjId: " + player.getObjectId() + " from DB", e);
		}
	}

	@Override
	public boolean insertBindPoint(Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
			
			BindPointPosition bpp = player.getBindPoint();
			stmt.setInt(1, player.getObjectId());
			stmt.setInt(2, bpp.getMapId());
			stmt.setFloat(3, bpp.getX());
			stmt.setFloat(4, bpp.getY());
			stmt.setFloat(5, bpp.getZ());
			stmt.setByte(6, bpp.getHeading());
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error("Could not store BindPointPosition data for player " + player.getObjectId() + " from DB", e);
			return false;
		}
	}

	@Override
	public boolean updateBindPoint(Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
			
			BindPointPosition bpp = player.getBindPoint();
			stmt.setInt(1, bpp.getMapId());
			stmt.setFloat(2, bpp.getX());
			stmt.setFloat(3, bpp.getY());
			stmt.setFloat(4, bpp.getZ());
			stmt.setByte(5, bpp.getHeading());
			stmt.setInt(6, player.getObjectId());
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error("Could not update BindPointPosition data for player " + player.getObjectId() + " from DB", e);
			return false;
		}
	}

	@Override
	public boolean store(final Player player) {
		boolean insert = false;
		BindPointPosition bind = player.getBindPoint();
		
		switch (bind.getPersistentState()) {
			case NEW:
				insert = insertBindPoint(player);
				break;
			case UPDATE_REQUIRED:
				insert = updateBindPoint(player);
				break;
			default:
				return true;
		}
		bind.setPersistentState(PersistentState.UPDATED);
		return insert;
	}
}