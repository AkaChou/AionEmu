package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.linked_skill.EquippedStigmasEntry;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MySQL8PlayerStigmasEquippedDAO extends PlayerStigmasEquippedDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerStigmasEquippedDAO.class);
    
    private static final String INSERT_QUERY = "INSERT INTO `player_stigmas_equipped` (`player_id`, `item_id`, `item_name`) VALUES (?,?,?)";
    
    private static final String UPDATE_QUERY = "UPDATE `player_stigmas_equipped` SET item_id=?, item_name=? WHERE player_id=? AND item_id=?";
    
    private static final String DELETE_QUERY = "DELETE FROM `player_stigmas_equipped` WHERE `player_id`=? AND item_id=? AND item_name=?";
    
    private static final String SELECT_QUERY = "SELECT `item_id`, `item_name` FROM `player_stigmas_equipped` WHERE `player_id`=?";
    
    private static final Predicate<EquippedStigmasEntry> itemsToInsertPredicate = new Predicate<EquippedStigmasEntry>() {
        @Override
        public boolean apply(@Nullable EquippedStigmasEntry input) {
            return input != null && PersistentState.NEW == input.getPersistentState();
        }
    };
    
    private static final Predicate<EquippedStigmasEntry> itemsToUpdatePredicate = new Predicate<EquippedStigmasEntry>() {
        @Override
        public boolean apply(@Nullable EquippedStigmasEntry input) {
            return input != null && PersistentState.UPDATE_REQUIRED == input.getPersistentState();
        }
    };
    
    private static final Predicate<EquippedStigmasEntry> itemsToDeletePredicate = new Predicate<EquippedStigmasEntry>() {
        @Override
        public boolean apply(@Nullable EquippedStigmasEntry input) {
            return input != null && PersistentState.DELETED == input.getPersistentState();
        }
    };
    
    @Override
    public PlayerEquippedStigmaList loadItemsList(int playerId) {
        List<EquippedStigmasEntry> items = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("item_id");
                    String name = rset.getString("item_name");
                    items.add(new EquippedStigmasEntry(id, name, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error("Could not restore StigmasEquipped data for player: {} from DB", playerId, e);
        }
        return new PlayerEquippedStigmaList(items);
    }
    
    @Override
    public boolean storeItems(Player player) {
        List<EquippedStigmasEntry> skillsActive = Lists.newArrayList(
            player.getEquipedStigmaList().getAllItems()
        );
        List<EquippedStigmasEntry> skillsDeleted = Lists.newArrayList(
            player.getEquipedStigmaList().getDeletedItems()
        );
        
        store(player, skillsActive);
        store(player, skillsDeleted);
        return true;
    }
    
    private void store(Player player, List<EquippedStigmasEntry> skills) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            deleteItems(con, player, skills);
            addItems(con, player, skills);
            updateItems(con, player, skills);
            
            con.commit();
        } catch (SQLException e) {
            log.error("Failed to save SkillList for player {}", player.getObjectId(), e);
            return;
        }
        
        for (EquippedStigmasEntry skill : skills) {
            skill.setPersistentState(PersistentState.UPDATED);
        }
    }
    
    private void addItems(Connection con, Player player, List<EquippedStigmasEntry> items) {
        Collection<EquippedStigmasEntry> skillsToInsert = Collections2.filter(items, itemsToInsertPredicate);
        if (GenericValidator.isBlankOrNull(skillsToInsert)) {
            return;
        }
        
        try (PreparedStatement ps = con.prepareStatement(INSERT_QUERY)) {
            for (EquippedStigmasEntry skill : skillsToInsert) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getItemId());
                ps.setString(3, skill.getItemName());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Error adding items for player {}", player.getObjectId(), e);
        }
    }
    
    private void updateItems(Connection con, Player player, List<EquippedStigmasEntry> skills) {
        Collection<EquippedStigmasEntry> skillsToUpdate = Collections2.filter(skills, itemsToUpdatePredicate);
        if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
            return;
        }
        
        try (PreparedStatement ps = con.prepareStatement(UPDATE_QUERY)) {
            for (EquippedStigmasEntry skill : skillsToUpdate) {
                ps.setInt(1, skill.getItemId());
                ps.setString(2, skill.getItemName());
                ps.setInt(3, player.getObjectId());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Error updating items for player {}", player.getObjectId(), e);
        }
    }

    private void deleteItems(Connection con, Player player, List<EquippedStigmasEntry> skills) {
        Collection<EquippedStigmasEntry> skillsToDelete = Collections2.filter(skills, itemsToDeletePredicate);
        if (GenericValidator.isBlankOrNull(skillsToDelete)) {
            return;
        }
        
        try (PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {
            for (EquippedStigmasEntry skill : skillsToDelete) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getItemId());
                ps.setString(3, skill.getItemName());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error("Error deleting items for player {}", player.getObjectId(), e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}