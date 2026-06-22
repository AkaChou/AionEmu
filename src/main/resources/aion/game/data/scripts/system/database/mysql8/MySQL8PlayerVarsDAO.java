package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerVarsDAO;
import javolution.util.FastMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author KID
 */
public class MySQL8PlayerVarsDAO extends PlayerVarsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerVarsDAO.class);

    private static final String SELECT_QUERY = "SELECT param,value FROM player_vars WHERE player_id=?";
    private static final String INSERT_QUERY = "INSERT INTO player_vars (`player_id`, `param`, `value`, `time`) VALUES (?,?,?,NOW())";
    private static final String DELETE_QUERY = "DELETE FROM player_vars WHERE player_id=? AND param=?";

    @Override
    public Map<String, Object> load(final int playerId) {
        final Map<String, Object> map = FastMap.newInstance();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(SELECT_QUERY)) {
            
            st.setInt(1, playerId);
            
            try (ResultSet rset = st.executeQuery()) {
                while (rset.next()) {
                    String key = rset.getString("param");
                    String value = rset.getString("value");
                    map.put(key, value);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading player vars for player: {}", playerId, e);
        }
        return map;
    }

    @Override
    public boolean set(final int playerId, final String key, final Object value) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setString(2, key);
            stmt.setString(3, value.toString());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error setting player var for player: {} key: {}", playerId, key, e);
            return false;
        }
    }

    @Override
    public boolean remove(final int playerId, final String key) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setString(2, key);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error removing player var for player: {} key: {}", playerId, key, e);
            return false;
        }
    }

    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL8DAOUtils.supports(s, i, i1);
    }
}