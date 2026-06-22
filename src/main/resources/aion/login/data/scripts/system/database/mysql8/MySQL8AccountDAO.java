package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DB;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.IUStH;
import com.aionemu.loginserver.dao.AccountDAO;
import com.aionemu.loginserver.model.Account;

/**
 * MySQL8 Account DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8AccountDAO extends AccountDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8AccountDAO.class);

    @Override
    public Account getAccount(String name) {
        String query = "SELECT * FROM account_data WHERE `name` = ?";
        Account account = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setString(1, name);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    account = new Account();
                    account.setId(rs.getInt("id"));
                    account.setName(name);
                    account.setPasswordHash(rs.getString("password"));
                    account.setAccessLevel(rs.getByte("access_level"));
                    account.setMembership(rs.getByte("membership"));
                    account.setActivated(rs.getByte("activated"));
                    account.setLastServer(rs.getByte("last_server"));
                    account.setLastIp(rs.getString("last_ip"));
                    account.setLastMac(rs.getString("last_mac"));
                    account.setIpForce(rs.getString("ip_force"));
                    account.setReturn(rs.getByte("return_account"));
                    account.setReturnEnd(rs.getTimestamp("return_end"));
                }
            }
        } catch (SQLException e) {
            log.error("Can't select account with name: " + name, e);
        }
        
        return account;
    }
    
    @Override
    public Account getAccount(int id) {
        String query = "SELECT * FROM account_data WHERE `id` = ?";
        Account account = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, id);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    account = new Account();
                    account.setId(rs.getInt("id"));
                    account.setName(rs.getString("name"));
                    account.setPasswordHash(rs.getString("password"));
                    account.setAccessLevel(rs.getByte("access_level"));
                    account.setMembership(rs.getByte("membership"));
                    account.setActivated(rs.getByte("activated"));
                    account.setLastServer(rs.getByte("last_server"));
                    account.setLastIp(rs.getString("last_ip"));
                    account.setLastMac(rs.getString("last_mac"));
                    account.setIpForce(rs.getString("ip_force"));
                    account.setReturn(rs.getByte("return_account"));
                    account.setReturnEnd(rs.getTimestamp("return_end"));
                }
            }
        } catch (SQLException e) {
            log.error("Can't select account with id: " + id, e);
        }
        
        return account;
    }

    @Override
    public int getAccountId(String name) {
        String query = "SELECT `id` FROM account_data WHERE `name` = ?";
        int id = -1;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setString(1, name);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            log.error("Can't select id for account: " + name, e);
        }
        
        return id;
    }

    @Override
    public int getAccountCount() {
        String query = "SELECT COUNT(*) AS c FROM account_data";
        int count = 0;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query);
             ResultSet rs = st.executeQuery()) {
            
            if (rs.next()) {
                count = rs.getInt("c");
            }
        } catch (SQLException e) {
            log.error("Can't get account count", e);
        }
        
        return count;
    }

    @Override
    public boolean insertAccount(Account account) {
        String query = "INSERT INTO account_data(`name`, `password`, access_level, membership, activated, " + "last_server, last_ip, last_mac, ip_force, toll) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            st.setString(1, account.getName());
            st.setString(2, account.getPasswordHash());
            st.setByte(3, account.getAccessLevel());
            st.setByte(4, account.getMembership());
            st.setByte(5, account.getActivated());
            st.setByte(6, account.getLastServer());
            st.setString(7, account.getLastIp());
            st.setString(8, account.getLastMac());
            st.setString(9, account.getIpForce());
            st.setLong(10, 0);
            
            int result = st.executeUpdate();
            
            if (result > 0) {
                try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        account.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("Can't insert account", e);
        }
        
        return false;
    }

    @Override
    public boolean updateAccount(Account account) {
        String query = "UPDATE account_data SET `name` = ?, `password` = ?, access_level = ?, " + "membership = ?, last_server = ?, last_ip = ?, last_mac = ?, ip_force = ?, " + "return_account = ?, return_end = ? WHERE `id` = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setString(1, account.getName());
            st.setString(2, account.getPasswordHash());
            st.setByte(3, account.getAccessLevel());
            st.setByte(4, account.getMembership());
            st.setByte(5, account.getLastServer());
            st.setString(6, account.getLastIp());
            st.setString(7, account.getLastMac());
            st.setString(8, account.getIpForce());
            st.setByte(9, account.getReturn());
            st.setTimestamp(10, account.getReturnEnd());
            st.setInt(11, account.getId());
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update account: " + account.getId(), e);
        }
        
        return false;
    }

    @Override
    public boolean updateLastServer(final int accountId, final byte lastServer) {
        String query = "UPDATE account_data SET last_server = ? WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setByte(1, lastServer);
            st.setInt(2, accountId);
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update last server for account: " + accountId, e);
        }
        
        return false;
    }

    @Override
    public boolean updateLastIp(final int accountId, final String ip) {
        String query = "UPDATE account_data SET last_ip = ? WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setString(1, ip);
            st.setInt(2, accountId);
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update last IP for account: " + accountId, e);
        }
        
        return false;
    }

    @Override
    public String getLastIp(final int accountId) {
        String query = "SELECT `last_ip` FROM `account_data` WHERE `id` = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, accountId);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("last_ip");
                }
            }
        } catch (SQLException e) {
            log.error("Can't select last IP for account: " + accountId, e);
        }
        
        return "";
    }
    
    @Override
    public boolean updateLastMac(final int accountId, final String mac) {
        String query = "UPDATE `account_data` SET `last_mac` = ? WHERE `id` = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setString(1, mac);
            st.setInt(2, accountId);
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update last MAC for account: " + accountId, e);
        }
        
        return false;
    }

    @Override
    public boolean updateMembership(final int accountId) {
        String query = "UPDATE account_data SET membership = old_membership, expire = NULL " + "WHERE id = ? AND expire < CURRENT_TIMESTAMP";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, accountId);
            
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Can't update membership for account: " + accountId, e);
        }
        
        return false;
    }

    @Override
    public void deleteInactiveAccounts(int daysOfInactivity) {
        String query = "DELETE FROM account_data WHERE id IN (" + "SELECT account_id FROM account_time " + "WHERE DATEDIFF(CURDATE(), last_active) > ?)";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, daysOfInactivity);
            st.executeUpdate();
        } catch (SQLException e) {
            log.error("Can't delete inactive accounts", e);
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
    }
}