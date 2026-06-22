package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.AccountPlayTimeDAO;
import com.aionemu.loginserver.model.AccountTime;

/**
 * MySQL8 Account PlayTime DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8AccountPlayTimeDAO extends AccountPlayTimeDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8AccountPlayTimeDAO.class);

    @Override
    public boolean update(final Integer accountId, final AccountTime accountTime) {
        String query = "INSERT INTO account_playtime (`account_id`, `accumulated_online`) VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE `accumulated_online` = `accumulated_online` + ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, accountId);
            st.setLong(2, accountTime.getAccumulatedOnlineTime());
            st.setLong(3, accountTime.getAccumulatedOnlineTime());
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update playtime for account: " + accountId, e);
        }
        
        return false;
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
    }
}