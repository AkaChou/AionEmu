package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.model.Petition;
import com.aionemu.gameserver.model.PetitionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zdead
 */
public class MySQL8PetitionDAO extends PetitionDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PetitionDAO.class);

    @Override
    public synchronized int getNextAvailableId() {
        String query = "SELECT COALESCE(MAX(id), 0) + 1 as nextid FROM petitions";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query);
             ResultSet rset = stmt.executeQuery()) {
            
            if (rset.next()) {
                return rset.getInt("nextid");
            }
        } catch (SQLException e) {
            log.error("Cannot get next available petition id", e);
        }
        return 0;
    }

    @Override
    public Petition getPetitionById(int petitionId) {
        String query = "SELECT * FROM petitions WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, petitionId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (!rset.next()) {
                    return null;
                }

                PetitionStatus status = getPetitionStatus(rset.getString("status"));
                return new Petition(
                    rset.getInt("id"),
                    rset.getInt("player_id"),
                    rset.getInt("type"),
                    rset.getString("title"),
                    rset.getString("message"),
                    rset.getString("add_data"),
                    status.getElementId()
                );
            }
        } catch (SQLException e) {
            log.error("Cannot get petition #{}", petitionId, e);
        }
        return null;
    }

    @Override
    public Set<Petition> getPetitions() {
        String query = "SELECT * FROM petitions WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY id ASC";
        Set<Petition> results = new HashSet<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query);
             ResultSet rset = stmt.executeQuery()) {
            
            while (rset.next()) {
                PetitionStatus status = getPetitionStatus(rset.getString("status"));
                Petition p = new Petition(
                    rset.getInt("id"),
                    rset.getInt("player_id"),
                    rset.getInt("type"),
                    rset.getString("title"),
                    rset.getString("message"),
                    rset.getString("add_data"),
                    status.getElementId()
                );
                results.add(p);
            }
        } catch (SQLException e) {
            log.error("Cannot get petitions", e);
            return null;
        }
        return results;
    }

    @Override
    public void deletePetition(int playerObjId) {
        String query = "DELETE FROM petitions WHERE player_id = ? AND status IN ('PENDING', 'IN_PROGRESS')";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, playerObjId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot delete petition", e);
        }
    }

    @Override
    public void insertPetition(Petition petition) {
        String query = "INSERT INTO petitions (id, player_id, type, title, message, add_data, time, status) VALUES(?,?,?,?,?,?,?,?)";
            
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, petition.getPetitionId());
            stmt.setInt(2, petition.getPlayerObjId());
            stmt.setInt(3, petition.getPetitionType().getElementId());
            stmt.setString(4, petition.getTitle());
            stmt.setString(5, petition.getContentText());
            stmt.setString(6, petition.getAdditionalData());
            stmt.setLong(7, System.currentTimeMillis() / 1000);
            stmt.setString(8, petition.getStatus().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot insert petition", e);
        }
    }

    @Override
    public void setReplied(int petitionId) {
        String query = "UPDATE petitions SET status = 'REPLIED' WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, petitionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot set petition replied", e);
        }
    }

    private PetitionStatus getPetitionStatus(String statusValue) {
        if ("PENDING".equals(statusValue)) {
            return PetitionStatus.PENDING;
        } else if ("IN_PROGRESS".equals(statusValue)) {
            return PetitionStatus.IN_PROGRESS;
        } else {
            return PetitionStatus.PENDING;
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}