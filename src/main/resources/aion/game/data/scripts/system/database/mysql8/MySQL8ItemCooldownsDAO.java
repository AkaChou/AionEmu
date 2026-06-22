package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.ItemCooldownsDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemCooldown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author ATracer
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8ItemCooldownsDAO extends ItemCooldownsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8ItemCooldownsDAO.class);

	private static final String INSERT_QUERY = "INSERT INTO `item_cooldowns` (`player_id`, `delay_id`, `use_delay`, `reuse_time`) VALUES (?,?,?,?) " + "ON DUPLICATE KEY UPDATE `use_delay` = VALUES(`use_delay`), `reuse_time` = VALUES(`reuse_time`)";
	private static final String DELETE_QUERY = "DELETE FROM `item_cooldowns` WHERE `player_id`=?";
	private static final String SELECT_QUERY = "SELECT `delay_id`, `use_delay`, `reuse_time` FROM `item_cooldowns` WHERE `player_id`=?";

	@Override
	public void loadItemCooldowns(final Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
			
			stmt.setInt(1, player.getObjectId());
			
			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					int delayId = rset.getInt("delay_id");
					int useDelay = rset.getInt("use_delay");
					long reuseTime = rset.getLong("reuse_time");
					if (reuseTime > System.currentTimeMillis()) {
						player.addItemCoolDown(delayId, reuseTime, useDelay);
					}
				}
			}
		} catch (SQLException e) {
			log.error("Error loading item cooldowns for player: " + player.getObjectId(), e);
		}
		player.getEffectController().broadCastEffects();
	}

	@Override
	public void storeItemCooldowns(Player player) {
		deleteItemCooldowns(player);
		
		Map<Integer, ItemCooldown> itemCoolDowns = player.getItemCoolDowns();
		if (itemCoolDowns == null || itemCoolDowns.isEmpty()) {
			return;
        }
		
		final Iterator<Map.Entry<Integer, ItemCooldown>> iterator = itemCoolDowns.entrySet().iterator();
		if (!iterator.hasNext()) {
			return;
		}

		try (Connection con = DatabaseFactory.getConnection()) {
			con.setAutoCommit(false);
			
			try (PreparedStatement st = con.prepareStatement(INSERT_QUERY)) {
				while (iterator.hasNext()) {
					Map.Entry<Integer, ItemCooldown> entry = iterator.next();
					ItemCooldown cooldown = entry.getValue();
					
					if (cooldown.getReuseTime() <= System.currentTimeMillis() + 30000) {
						continue;
					}
					
					st.setInt(1, player.getObjectId());
					st.setInt(2, entry.getKey());
					st.setInt(3, cooldown.getUseDelay());
					st.setLong(4, cooldown.getReuseTime());
					st.addBatch();
				}
				st.executeBatch();
			}
			
			con.commit();
		} catch (SQLException e) {
			log.error("Error while storing item cooldowns for player " + player.getObjectId(), e);
		}
	}

	private void deleteItemCooldowns(final Player player) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
			
			stmt.setInt(1, player.getObjectId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Error deleting item cooldowns for player: " + player.getObjectId(), e);
		}
	}

	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return MySQL8DAOUtils.supports(arg0, arg1, arg2);
	}
}