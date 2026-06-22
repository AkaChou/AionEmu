package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.TownDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.town.Town;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL 8 implementation of TownDAO
 * @author ViAl, Updated for MySQL 8
 */
public class MySQL8TownDAO extends TownDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8TownDAO.class);

    private static final String SELECT_QUERY = "SELECT * FROM `towns` WHERE `race` = ? ORDER BY `id`";
    private static final String SELECT_ALL_QUERY = "SELECT * FROM `towns` ORDER BY `race`, `id`";
    private static final String INSERT_QUERY = "INSERT INTO `towns` (`id`, `level`, `points`, `race`, `level_up_date`) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE `towns` SET `level` = ?, `points` = ?, `level_up_date` = ? WHERE `id` = ?";
    private static final String UPSERT_QUERY = "INSERT INTO `towns` (`id`, `level`, `points`, `race`, `level_up_date`) VALUES (?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE `level` = VALUES(`level`), `points` = VALUES(`points`), `level_up_date` = VALUES(`level_up_date`)";

    @Override
    public Map<Integer, Town> load(Race race) {
        Map<Integer, Town> towns = new ConcurrentHashMap<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setString(1, race.toString());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    Town town = extractTownFromResultSet(rset);
                    towns.put(town.getId(), town);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to load towns for race: {}", race, e);
        }
        
        return towns;
    }

    public Map<Integer, Town> loadAll() {
        Map<Integer, Town> towns = new ConcurrentHashMap<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ALL_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            while (rset.next()) {
                Town town = extractTownFromResultSet(rset);
                towns.put(town.getId(), town);
            }
            
        } catch (SQLException e) {
            log.error("Failed to load all towns", e);
        }
        
        return towns;
    }

    private Town extractTownFromResultSet(ResultSet rset) throws SQLException {
        int id = rset.getInt("id");
        int level = rset.getInt("level");
        int points = rset.getInt("points");
        Race race = Race.valueOf(rset.getString("race"));
        Timestamp levelUpDate = rset.getTimestamp("level_up_date");
        
        Town town = new Town(id, level, points, race, levelUpDate);
        town.setPersistentState(PersistentState.UPDATED);
        
        return town;
    }

    @Override
    public void store(Town town) {
        if (town == null) {
            return;
        }

        switch (town.getPersistentState()) {
            case NEW:
                insertTown(town);
                break;
            case UPDATE_REQUIRED:
                updateTown(town);
                break;
            default:
                break;
        }
    }

    private void insertTown(Town town) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            setTownStatementParameters(stmt, town);
            stmt.executeUpdate();
            town.setPersistentState(PersistentState.UPDATED);
            
        } catch (SQLException e) {
            log.error("Failed to insert town: {}", town.getId(), e);
        }
    }

    private void updateTown(Town town) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, town.getLevel());
            stmt.setInt(2, town.getPoints());
            
            Timestamp levelUpDate = town.getLevelUpDate();
            if (levelUpDate == null || levelUpDate.getTime() < 1000000) {
                stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            } else {
                stmt.setTimestamp(3, levelUpDate);
            }
            
            stmt.setInt(4, town.getId());
            
            int updated = stmt.executeUpdate();
            
            if (updated == 0) {
                insertTown(town);
            } else {
                town.setPersistentState(PersistentState.UPDATED);
            }
            
        } catch (SQLException e) {
            log.error("Failed to update town: {}", town.getId(), e);
        }
    }

    public void upsertTown(Town town) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPSERT_QUERY)) {
            
            setTownStatementParameters(stmt, town);
            stmt.executeUpdate();
            town.setPersistentState(PersistentState.UPDATED);
            
        } catch (SQLException e) {
            log.error("Failed to upsert town: {}", town.getId(), e);
        }
    }

    private void setTownStatementParameters(PreparedStatement stmt, Town town) throws SQLException {
        stmt.setInt(1, town.getId());
        stmt.setInt(2, town.getLevel());
        stmt.setInt(3, town.getPoints());
        stmt.setString(4, town.getRace().toString());
        
        Timestamp levelUpDate = town.getLevelUpDate();
        if (levelUpDate == null || levelUpDate.getTime() < 1000000) {
            stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
        } else {
            stmt.setTimestamp(5, levelUpDate);
        }
    }

    public void storeTowns(Iterable<Town> towns) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(UPSERT_QUERY)) {
                int batchCount = 0;
                
                for (Town town : towns) {
                    if (town == null) {
                        continue;
                    }
                    
                    setTownStatementParameters(stmt, town);
                    stmt.addBatch();
                    batchCount++;
                    
                    if (batchCount % 50 == 0) {
                        stmt.executeBatch();
                    }
                }
                
                if (batchCount % 50 != 0) {
                    stmt.executeBatch();
                }
            }
            
            con.commit();
            
            for (Town town : towns) {
                if (town != null) {
                    town.setPersistentState(PersistentState.UPDATED);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to batch store towns", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}