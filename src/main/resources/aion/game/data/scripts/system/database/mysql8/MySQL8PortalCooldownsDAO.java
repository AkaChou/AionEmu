package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PortalCooldownsDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PortalCooldownItem;
import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class MySQL8PortalCooldownsDAO extends PortalCooldownsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8PortalCooldownsDAO.class);

	private static final String INSERT_QUERY = "INSERT INTO `portal_cooldowns` (`player_id`, `world_id`, `reuse_time`, `entry_count`) VALUES (?,?,?,?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`), `entry_count` = VALUES(`entry_count`)";
	private static final String DELETE_QUERY = "DELETE FROM `portal_cooldowns` WHERE `player_id`=?";
	private static final String SELECT_QUERY = "SELECT `world_id`, `reuse_time`, `entry_count` FROM `portal_cooldowns` WHERE `player_id`=?";

	@Override
	public void loadPortalCooldowns(final Player player) {
		FastMap<Integer, PortalCooldownItem> portalCoolDowns = new FastMap<Integer, PortalCooldownItem>();
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			
			stmt.setInt(1, player.getObjectId());
			
			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int worldId = rset.getInt("world_id");
					long reuseTime = rset.getLong("reuse_time");
					int entryCount = rset.getInt("entry_count");
					if (reuseTime > System.currentTimeMillis()) {
						portalCoolDowns.put(worldId, new PortalCooldownItem(worldId, entryCount, reuseTime));
					}
				}
			}
			
			player.getPortalCooldownList().setPortalCoolDowns(portalCoolDowns);
		} catch (SQLException e) {
			log.error("LoadPortalCooldowns for player: " + player.getObjectId(), e);
		}
	}

	@Override
	public void storePortalCooldowns(final Player player) {
		deletePortalCooldowns(player);
		
		Map<Integer, PortalCooldownItem> portalCoolDowns = player.getPortalCooldownList().getPortalCoolDowns();
		if (portalCoolDowns == null || portalCoolDowns.isEmpty()) {
			return;
        }
		
		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			
			try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
				for (Map.Entry<Integer, PortalCooldownItem> entry : portalCoolDowns.entrySet()) {
					final int worldId = entry.getKey();
					final PortalCooldownItem item = entry.getValue();
					final long reuseTime = item.getCooldown();
					final int entryCount = item.getEntryCount();

					if (reuseTime < System.currentTimeMillis()) {
						continue;
					}
					
					stmt.setInt(1, player.getObjectId());
					stmt.setInt(2, worldId);
					stmt.setLong(3, reuseTime);
					stmt.setInt(4, entryCount);
					stmt.addBatch();
				}
				stmt.executeBatch();
			}
			
			con.commit();
		} catch (SQLException e) {
			log.error("storePortalCooldowns for player: " + player.getObjectId(), e);
		}
	}

	private void deletePortalCooldowns(final Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
			
			stmt.setInt(1, player.getObjectId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("deletePortalCooldowns for player: " + player.getObjectId(), e);
		}
	}

	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return MySQL8DAOUtils.supports(arg0, arg1, arg2);
	}
}