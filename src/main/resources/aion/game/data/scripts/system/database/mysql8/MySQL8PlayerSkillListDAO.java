package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
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

/**
 * @author SoulKeeper
 * @author IceReaper, orfeo087, Avol, AEJTester
 * Updated for MySQL 8 support
 */
public class MySQL8PlayerSkillListDAO extends PlayerSkillListDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerSkillListDAO.class);
    
    public static final String INSERT_QUERY = "INSERT INTO `player_skills` (`player_id`, `skill_id`, `skill_level`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `skill_level` = VALUES(`skill_level`)";
    
    public static final String UPDATE_QUERY = "UPDATE `player_skills` SET skill_level = ? WHERE player_id = ? AND skill_id = ?";
    
    public static final String UPDATE_SKIN_QUERY = "UPDATE `player_skills` SET skin_id = ?, skin_active_date = ?, " + "skin_expire_time = ?, skin_activated = ? WHERE player_id = ? AND skill_id = ?";
    
    public static final String DELETE_QUERY = "DELETE FROM `player_skills` WHERE `player_id` = ? AND skill_id = ?";
    
    public static final String SELECT_QUERY = "SELECT `skill_id`, `skill_level`, `skin_id`, `skin_active_date`, " + "`skin_expire_time`, `skin_activated` FROM `player_skills` WHERE `player_id` = ?";

    private static final Predicate<PlayerSkillEntry> skillsToInsertPredicate = new Predicate<PlayerSkillEntry>() {
        @Override
        public boolean apply(@Nullable PlayerSkillEntry input) {
            return input != null && PersistentState.NEW == input.getPersistentState();
        }
    };

    private static final Predicate<PlayerSkillEntry> skillsToUpdatePredicate = new Predicate<PlayerSkillEntry>() {
        @Override
        public boolean apply(@Nullable PlayerSkillEntry input) {
            return input != null && PersistentState.UPDATE_REQUIRED == input.getPersistentState();
        }
    };

    private static final Predicate<PlayerSkillEntry> skillsToDeletePredicate = new Predicate<PlayerSkillEntry>() {
        @Override
        public boolean apply(@Nullable PlayerSkillEntry input) {
            return input != null && PersistentState.DELETED == input.getPersistentState();
        }
    };

    @Override
    public PlayerSkillList loadSkillList(int playerId) {
        List<PlayerSkillEntry> skills = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("skill_id");
                    int lv = rset.getInt("skill_level");
                    int skin_id = rset.getInt("skin_id");
                    Timestamp active_date = rset.getTimestamp("skin_active_date");
                    int expireTime = rset.getInt("skin_expire_time");
                    boolean isActivated = rset.getBoolean("skin_activated");

                    skills.add(new PlayerSkillEntry(id, false, false, lv, skin_id, active_date, expireTime, isActivated, PersistentState.UPDATED));
                }
            }
        } catch (Exception e) {
            log.error("Could not restore SkillList data for player: {}", playerId, e);
        }
        
        return new PlayerSkillList(skills);
    }

    @Override
    public boolean storeSkills(Player player) {
        List<PlayerSkillEntry> skillsActive = Lists.newArrayList(player.getSkillList().getAllSkills());
        List<PlayerSkillEntry> skillsDeleted = Lists.newArrayList(player.getSkillList().getDeletedSkills());
        
        store(player, skillsActive);
        store(player, skillsDeleted);

        return true;
    }

    private void store(Player player, List<PlayerSkillEntry> skills) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            deleteSkills(con, player, skills);
            addSkills(con, player, skills);
            updateSkills(con, player, skills);
            updateSkinSkills(con, player, skills);
            
            con.commit();
        } catch (SQLException e) {
            log.error("Failed to save SkillList for player {}", player.getObjectId(), e);
            try (Connection con = DatabaseFactory.getConnection()) {
                con.rollback();
            } catch (SQLException rollbackEx) {
                log.error("Failed to rollback skills transaction for player {}", player.getObjectId(), rollbackEx);
            }
        }
        
        for (PlayerSkillEntry skill : skills) {
            skill.setPersistentState(PersistentState.UPDATED);
        }
    }

    private void addSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
        
        Collection<PlayerSkillEntry> skillsToInsert = Collections2.filter(skills, skillsToInsertPredicate);
        
        if (GenericValidator.isBlankOrNull(skillsToInsert)) {
            return;
        }
        
        try (PreparedStatement ps = con.prepareStatement(INSERT_QUERY)) {
            for (PlayerSkillEntry skill : skillsToInsert) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getSkillId());
                ps.setInt(3, skill.getSkillLevel());
                ps.addBatch();
            }
            
            ps.executeBatch();
        }
    }

    private void updateSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
        
        Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);
        
        if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
            return;
        }
        
        try (PreparedStatement ps = con.prepareStatement(UPDATE_QUERY)) {
            for (PlayerSkillEntry skill : skillsToUpdate) {
                ps.setInt(1, skill.getSkillLevel());
                ps.setInt(2, player.getObjectId());
                ps.setInt(3, skill.getSkillId());
                ps.addBatch();
            }
            
            ps.executeBatch();
        }
    }
    
    private void updateSkinSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
        
        Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);
        
        if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
            return;
        }
        
        try (PreparedStatement ps = con.prepareStatement(UPDATE_SKIN_QUERY)) {
            for (PlayerSkillEntry skill : skillsToUpdate) {
                ps.setInt(1, skill.getSkinId());
                ps.setTimestamp(2, skill.getSkinActiveTime());
                ps.setInt(3, skill.getSkinExpireTime());
                ps.setBoolean(4, skill.isActivated());
                ps.setInt(5, player.getObjectId());
                ps.setInt(6, skill.getSkillId());
                ps.addBatch();
            }
            
            ps.executeBatch();
        }
    }

    private void deleteSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {
        
        Collection<PlayerSkillEntry> skillsToDelete = Collections2.filter(skills, skillsToDeletePredicate);
        
        if (GenericValidator.isBlankOrNull(skillsToDelete)) {
            return;
        }
        
        try (PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {
            for (PlayerSkillEntry skill : skillsToDelete) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getSkillId());
                ps.addBatch();
            }
            
            ps.executeBatch();
        }
    }

    @Override
    public Timestamp getSkinSkillActiveDateById(int playerObjId, int skillId) {
        String query = "SELECT `skin_active_date` FROM `player_skills` " + "WHERE `player_id` = ? AND `skill_id` = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {
            
            s.setInt(1, playerObjId);
            s.setInt(2, skillId);
            
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("skin_active_date");
                }
            }
        } catch (Exception e) {
            log.error("Error getting skin skill active date for player {} skill {}", playerObjId, skillId, e);
        }
        
        return null;
    }

    @Override
    public int getSkinExpireTime(int playerObjId, int skillId) {
        String query = "SELECT `skin_expire_time` FROM `player_skills` " + "WHERE `player_id` = ? AND `skill_id` = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {
            
            s.setInt(1, playerObjId);
            s.setInt(2, skillId);
            
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("skin_expire_time");
                }
            }
        } catch (Exception e) {
            log.error("Error getting skin skill expire time for player {} skill {}", playerObjId, skillId, e);
        }
        
        return 0;
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}