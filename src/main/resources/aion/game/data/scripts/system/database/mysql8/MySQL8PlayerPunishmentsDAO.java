package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.model.account.CharacterBanInfo;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.PunishmentService.PunishmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author lord_rex, Cura, nrg
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerPunishmentsDAO extends PlayerPunishmentsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerPunishmentsDAO.class);
	
	private static final String SELECT_QUERY = "SELECT `player_id`, `start_time`, `duration`, `reason` FROM `player_punishments` WHERE `player_id`=? AND `punishment_type`=?";
	private static final String UPDATE_QUERY = "UPDATE `player_punishments` SET `duration`=? WHERE `player_id`=? AND `punishment_type`=?";
	private static final String REPLACE_QUERY = "REPLACE INTO `player_punishments` VALUES (?,?,?,?,?)";
	private static final String DELETE_QUERY = "DELETE FROM `player_punishments` WHERE `player_id`=? AND `punishment_type`=?";

	@Override
	public void loadPlayerPunishments(final Player player, final PunishmentType punishmentType) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {
			
			ps.setInt(1, player.getObjectId());
			ps.setString(2, punishmentType.toString());
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (punishmentType == PunishmentType.PRISON) {
						player.setPrisonTimer(rs.getLong("duration") * 1000);
					} else if (punishmentType == PunishmentType.GATHER) {
						player.setGatherableTimer(rs.getLong("duration") * 1000);
					}
				}
			}
		} catch (SQLException e) {
			log.error("Error loading player punishments for player: " + player.getObjectId(), e);
		}
	}

	@Override
	public void storePlayerPunishments(final Player player, final PunishmentType punishmentType) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(UPDATE_QUERY)) {
			
			if (punishmentType == PunishmentType.PRISON) {
				ps.setLong(1, player.getPrisonTimer() / 1000);
			} else if (punishmentType == PunishmentType.GATHER) {
				ps.setLong(1, (player.getGatherableTimer() - (System.currentTimeMillis() - player.getStopGatherable())) / 1000);
			}
			ps.setInt(2, player.getObjectId());
			ps.setString(3, punishmentType.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error storing player punishments for player: " + player.getObjectId(), e);
		}
	}
	
	@Override
	public void punishPlayer(final int playerId, final PunishmentType punishmentType, final long duration, final String reason) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(REPLACE_QUERY)) {
			
			ps.setInt(1, playerId);
			ps.setString(2, punishmentType.toString());
			ps.setLong(3, System.currentTimeMillis() / 1000);
			ps.setLong(4, duration);
			ps.setString(5, reason);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error punishing player: " + playerId, e);
		}
	}

	@Override
	public void punishPlayer(final Player player, final PunishmentType punishmentType, final String reason) {
		if (punishmentType == PunishmentType.PRISON) {
			punishPlayer(player.getObjectId(), punishmentType, player.getPrisonTimer() / 1000, reason);
		} else if (punishmentType == PunishmentType.GATHER) {
			punishPlayer(player.getObjectId(), punishmentType, player.getGatherableTimer() / 1000, reason);
		}
	}

	@Override
	public void unpunishPlayer(final int playerId, final PunishmentType punishmentType) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {
			
			ps.setInt(1, playerId);
			ps.setString(2, punishmentType.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error unpunishing player: " + playerId, e);
		}
	}
	
	@Override
	public CharacterBanInfo getCharBanInfo(final int playerId) {
		final CharacterBanInfo[] charBan = new CharacterBanInfo[1];
		
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {
			
			ps.setInt(1, playerId);
			ps.setString(2, PunishmentType.CHARBAN.toString());
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					charBan[0] = new CharacterBanInfo(playerId, rs.getLong("start_time"), rs.getLong("duration"), rs.getString("reason"));
				}
			}
		} catch (SQLException e) {
			log.error("Error getting char ban info for player: " + playerId, e);
		}
		return charBan[0];
	}

	@Override
	public boolean supports(String s, int i, int i1) {
		return MySQL8DAOUtils.supports(s, i, i1);
	}
}