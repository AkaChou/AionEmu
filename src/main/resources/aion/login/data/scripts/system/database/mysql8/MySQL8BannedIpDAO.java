package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.BannedIpDAO;
import com.aionemu.loginserver.model.BannedIP;

/**
 * MySQL8 BannedIP DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8BannedIpDAO extends BannedIpDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8BannedIpDAO.class);

    @Override
    public BannedIP insert(String mask) {
        return insert(mask, null);
    }

    @Override
    public BannedIP insert(final String mask, final Timestamp expireTime) {
        BannedIP result = new BannedIP();
        result.setMask(mask);
        result.setTimeEnd(expireTime);
        
        return insert(result) ? result : null;
    }

    @Override
    public boolean insert(final BannedIP bannedIP) {
        String query = "INSERT INTO banned_ip(mask, time_end) VALUES (?, ?)";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            st.setString(1, bannedIP.getMask());
            
            if (bannedIP.getTimeEnd() == null) {
                st.setNull(2, Types.TIMESTAMP);
            } else {
                st.setTimestamp(2, bannedIP.getTimeEnd());
            }
            
            int affected = st.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bannedIP.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Can't insert banned IP: " + bannedIP.getMask(), e);
        }
        
        return false;
    }

    @Override
    public boolean update(final BannedIP bannedIP) {
        String query = "UPDATE banned_ip SET mask = ?, time_end = ? WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setString(1, bannedIP.getMask());
            
            if (bannedIP.getTimeEnd() == null) {
                st.setNull(2, Types.TIMESTAMP);
            } else {
                st.setTimestamp(2, bannedIP.getTimeEnd());
            }
            
            st.setInt(3, bannedIP.getId());
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update banned IP: " + bannedIP.getId(), e);
        }
        
        return false;
    }

    @Override
    public boolean remove(final String mask) {
        String query = "DELETE FROM banned_ip WHERE mask = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setString(1, mask);
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't remove banned IP: " + mask, e);
        }
        
        return false;
    }

    @Override
    public boolean remove(final BannedIP bannedIP) {
        return remove(bannedIP.getMask());
    }

    @Override
    public Set<BannedIP> getAllBans() {
        String query = "SELECT * FROM banned_ip ORDER BY id";
        Set<BannedIP> result = new HashSet<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query);
             ResultSet rs = st.executeQuery()) {
            
            while (rs.next()) {
                BannedIP ip = new BannedIP();
                ip.setId(rs.getInt("id"));
                ip.setMask(rs.getString("mask"));
                ip.setTimeEnd(rs.getTimestamp("time_end"));
                result.add(ip);
            }
        } catch (SQLException e) {
            log.error("Can't get all banned IPs", e);
        }
        
        return result;
    }

    @Override
    public void cleanExpiredBans() {
        String query = "DELETE FROM banned_ip WHERE time_end < CURRENT_TIMESTAMP AND time_end IS NOT NULL";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            int deleted = st.executeUpdate();
            if (deleted > 0) {
                log.info("Cleaned " + deleted + " expired IP bans");
            }
        } catch (SQLException e) {
            log.error("Can't clean expired IP bans", e);
        }
    }

    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL8DAOUtils.supports(s, i, i1);
    }
}