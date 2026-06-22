package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerPasskeyDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class MySQL8PlayerPasskeyDAO extends PlayerPasskeyDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerPasskeyDAO.class);
    
    private static final String INSERT_QUERY = "INSERT INTO `player_passkey` (`account_id`, `passkey`) VALUES (?,?)";
    private static final String UPDATE_QUERY = "UPDATE `player_passkey` SET `passkey`=? WHERE `account_id`=? AND `passkey`=?";
    private static final String UPDATE_FORCE_QUERY = "UPDATE `player_passkey` SET `passkey`=? WHERE `account_id`=?";
    private static final String CHECK_QUERY = "SELECT COUNT(*) as cnt FROM `player_passkey` WHERE `account_id`=? AND `passkey`=?";
    private static final String EXIST_CHECK_QUERY = "SELECT COUNT(*) as cnt FROM `player_passkey` WHERE `account_id`=?";
    
    @Override
    public void insertPlayerPasskey(int accountId, String passkey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setString(2, passkey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error saving PlayerPasskey. accountId: {}", accountId, e);
        }
    }
    
    @Override
    public boolean updatePlayerPasskey(int accountId, String oldPasskey, String newPasskey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setString(1, newPasskey);
            stmt.setInt(2, accountId);
            stmt.setString(3, oldPasskey);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating PlayerPasskey. accountId: {}", accountId, e);
            return false;
        }
    }
    
    @Override
    public boolean updateForcePlayerPasskey(int accountId, String newPasskey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_FORCE_QUERY)) {
            
            stmt.setString(1, newPasskey);
            stmt.setInt(2, accountId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating PlayerPasskey. accountId: {}", accountId, e);
            return false;
        }
    }
    
    @Override
    public boolean checkPlayerPasskey(int accountId, String passkey) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CHECK_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setString(2, passkey);
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("cnt") == 1;
                }
            }
        } catch (SQLException e) {
            log.error("Error loading PlayerPasskey. accountId: {}", accountId, e);
        }
        return false;
    }
    
    @Override
    public boolean existCheckPlayerPasskey(int accountId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(EXIST_CHECK_QUERY)) {
            
            stmt.setInt(1, accountId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("cnt") == 1;
                }
            }
        } catch (SQLException e) {
            log.error("Error loading PlayerPasskey. accountId: {}", accountId, e);
        }
        return false;
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}