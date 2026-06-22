package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerSkillSkinListDAO;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class MySQL8PlayerSkillSkinListDAO extends PlayerSkillSkinListDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerSkillSkinListDAO.class);
    
    private static final String LOAD_QUERY = "SELECT `skin_id`, `remaining`, `active` FROM `player_skill_skins` WHERE `player_id`=?";
    private static final String INSERT_QUERY = "INSERT INTO `player_skill_skins`(`player_id`, `skin_id`, `remaining`, `active`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`), `active` = VALUES(`active`)";
    private static final String DELETE_QUERY = "DELETE FROM `player_skill_skins` WHERE `player_id`=? AND `skin_id`=?";
    private static final String UPDATE_ACTIVE_QUERY = "UPDATE `player_skill_skins` SET `active` = ? WHERE `player_id`=? AND `skin_id`=?";

    @Override
    public SkillSkinList loadSkillSkinList(final int playerId) {
        final SkillSkinList tl = new SkillSkinList();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOAD_QUERY)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("skin_id");
                    int remaining = rset.getInt("remaining");
                    int active = rset.getInt("active");
                    tl.addEntry(id, remaining, active);
                }
            }
        } catch (SQLException e) {
            log.error("Could not load skill skin list for player {} from DB", playerId, e);
        }
        
        return tl;
    }

    @Override
    public boolean storeSkillSkins(Player player, SkillSkin entry) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, entry.getId());
            stmt.setInt(3, entry.getExpireTime());
            stmt.setInt(4, entry.getIsActive());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not store skill skin for player {} from DB", player.getObjectId(), e);
            return false;
        }
    }

    @Override
    public boolean setActive(final int playerObjId, final int skinId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ACTIVE_QUERY)) {
            
            stmt.setInt(1, 1); // active = 1
            stmt.setInt(2, playerObjId);
            stmt.setInt(3, skinId);
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            log.error("Could not set active skill skin for player {} skin {}", playerObjId, skinId, e);
            return false;
        }
    }

    @Override
    public boolean setDeactive(final int playerObjId, final int skinId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ACTIVE_QUERY)) {
            
            stmt.setInt(1, 0); // active = 0
            stmt.setInt(2, playerObjId);
            stmt.setInt(3, skinId);
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            log.error("Could not deactivate skill skin for player {} skin {}", playerObjId, skinId, e);
            return false;
        }
    }

    @Override
    public boolean removeSkillSkin(int playerId, int skinId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, skinId);
            int deleted = stmt.executeUpdate();
            return deleted > 0;
        } catch (SQLException e) {
            log.error("Could not delete skill skin for player {} from DB", playerId, e);
            return false;
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}