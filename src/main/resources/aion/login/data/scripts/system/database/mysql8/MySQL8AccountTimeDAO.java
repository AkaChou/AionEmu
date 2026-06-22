package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.AccountTimeDAO;
import com.aionemu.loginserver.model.AccountTime;

/**
 * MySQL8 AccountTime DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8AccountTimeDAO extends AccountTimeDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8AccountTimeDAO.class);

    @Override
    public boolean updateAccountTime(final int accountId, final AccountTime accountTime) {
        String query = "REPLACE INTO account_time (account_id, last_active, expiration_time, " + "session_duration, accumulated_online, accumulated_rest, penalty_end) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setLong(1, accountId);
            st.setTimestamp(2, accountTime.getLastLoginTime());
            st.setTimestamp(3, accountTime.getExpirationTime());
            st.setLong(4, accountTime.getSessionDuration());
            st.setLong(5, accountTime.getAccumulatedOnlineTime());
            st.setLong(6, accountTime.getAccumulatedRestTime());
            st.setTimestamp(7, accountTime.getPenaltyEnd());
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update account time for account: " + accountId, e);
        }
        
        return false;
    }

    @Override
    public AccountTime getAccountTime(int accountId) {
        String query = "SELECT * FROM account_time WHERE account_id = ?";
        AccountTime accountTime = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setLong(1, accountId);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    accountTime = new AccountTime();
                    accountTime.setLastLoginTime(rs.getTimestamp("last_active"));
                    accountTime.setSessionDuration(rs.getLong("session_duration"));
                    accountTime.setAccumulatedOnlineTime(rs.getLong("accumulated_online"));
                    accountTime.setAccumulatedRestTime(rs.getLong("accumulated_rest"));
                    accountTime.setPenaltyEnd(rs.getTimestamp("penalty_end"));
                    accountTime.setExpirationTime(rs.getTimestamp("expiration_time"));
                }
            }
        } catch (SQLException e) {
            log.error("Can't get account time for account: " + accountId, e);
        }
        
        return accountTime;
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
    }
}