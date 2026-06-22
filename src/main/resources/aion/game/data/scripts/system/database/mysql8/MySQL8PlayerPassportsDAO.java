package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerPassportsDAO;

/**
 * @author Alcapwnd, Lyras, FrozenKiller
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerPassportsDAO extends PlayerPassportsDAO {

	private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerPassportsDAO.class);

	private static final String INSERT_QUERY = "INSERT INTO `player_passports` (`account_id`, `passport_id`, `stamps`, `last_stamp`) VALUES (?,?,?,?)";
	private static final String UPDATE_QUERY = "UPDATE player_passports SET stamps = ?, rewarded = ?, last_stamp = ? WHERE account_id = ? AND passport_id = ?";
	private static final String SELECT_STAMPS_QUERY = "SELECT stamps FROM player_passports WHERE account_id = ? AND passport_id = ?";
	private static final String SELECT_LAST_STAMP_QUERY = "SELECT last_stamp FROM player_passports WHERE account_id = ? AND passport_id = ?";
	private static final String SELECT_PASSPORTS_QUERY = "SELECT passport_id FROM player_passports WHERE account_id = ?";

	@Override
	public void insertPassport(final int accountId, final int passportId, final int stamps, final Timestamp last_stamp) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
			
			stmt.setInt(1, accountId);
			stmt.setInt(2, passportId);
			stmt.setInt(3, stamps);
			stmt.setTimestamp(4, last_stamp);
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error("Can't insert into passports: " + e.getMessage(), e);
		}
	}

	@Override
	public void updatePassport(final int accountId, final int passportId, final int stamps, final boolean rewarded, final Timestamp last_stamp) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
			
			stmt.setInt(1, stamps);
			stmt.setInt(2, rewarded ? 1 : 0);
			stmt.setTimestamp(3, last_stamp);
			stmt.setInt(4, accountId);
			stmt.setInt(5, passportId);
			stmt.executeUpdate();
		} catch (Exception e) {
			log.error("Error updating passports ", e);
		}
	}

	@Override
	public int getStamps(final int accountId, final int passportId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement s = con.prepareStatement(SELECT_STAMPS_QUERY)) {
			
			s.setInt(1, accountId);
			s.setInt(2, passportId);
			
			try (ResultSet rs = s.executeQuery()) {
				if (rs.next()) {
					return rs.getInt("stamps");
				}
			}
		} catch (SQLException e) {
			log.error("Error getting stamps for account: " + accountId + ", passport: " + passportId, e);
		}
		return 0;
	}

	@Override
	public Timestamp getLastStamp(final int accountId, final int passportId) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement s = con.prepareStatement(SELECT_LAST_STAMP_QUERY)) {
			
			s.setInt(1, accountId);
			s.setInt(2, passportId);
			
			try (ResultSet rs = s.executeQuery()) {
				if (rs.next()) {
					return rs.getTimestamp("last_stamp");
				}
			}
		} catch (SQLException e) {
			log.error("Can't get last Passport Stamp! for account: " + accountId, e);
		}
		return new Timestamp(System.currentTimeMillis());
	}

	@Override
	public List<Integer> getPassports(final int accountId) {
		final List<Integer> ids = new ArrayList<Integer>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_PASSPORTS_QUERY)) {
			
			stmt.setInt(1, accountId);
			
			try (ResultSet resultSet = stmt.executeQuery()) {
				while (resultSet.next()) {
					ids.add(resultSet.getInt("passport_id"));
				}
			}
		} catch (SQLException e) {
			log.error("Error getting passports for account: " + accountId, e);
		}
		return ids;
	}

	@Override
	public boolean supports(String s, int i, int i1) {
		return MySQL8DAOUtils.supports(s, i, i1);
	}
}