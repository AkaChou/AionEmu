package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.dao.GameServersDAO;

/**
 * MySQL8 GameServers DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8GameServersDAO extends GameServersDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8GameServersDAO.class);

    @Override
    public Map<Byte, GameServerInfo> getAllGameServers() {
        Map<Byte, GameServerInfo> result = new HashMap<>();
        String query = "SELECT * FROM gameservers ORDER BY id";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query);
             ResultSet rs = st.executeQuery()) {
            
            while (rs.next()) {
                byte id = rs.getByte("id");
                String ipMask = rs.getString("mask");
                String password = rs.getString("password");
                GameServerInfo gsi = new GameServerInfo(id, ipMask, password);
                result.put(id, gsi);
            }
        } catch (SQLException e) {
            log.error("Error loading game servers", e);
        }
        
        return result;
    }

    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL8DAOUtils.supports(s, i, i1);
    }
}