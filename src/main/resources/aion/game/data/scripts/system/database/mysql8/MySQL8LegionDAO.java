package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.*;
import javolution.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List; 
import java.util.TreeMap;

/**
 * @author Simple
 * @modified cura
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8LegionDAO extends LegionDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8LegionDAO.class);
    
    private static final String INSERT_LEGION_QUERY = "INSERT INTO legions (id, `name`) VALUES (?, ?)";
    private static final String SELECT_LEGION_QUERY1 = "SELECT * FROM legions WHERE id = ?";
    private static final String SELECT_LEGION_QUERY2 = "SELECT * FROM legions WHERE name = ?";
    private static final String DELETE_LEGION_QUERY = "DELETE FROM legions WHERE id = ?";
    private static final String UPDATE_LEGION_QUERY = "UPDATE legions SET name = ?, level = ?, contribution_points = ?, " + "deputy_permission = ?, centurion_permission = ?, legionary_permission = ?, " + "volunteer_permission = ?, disband_time = ?, description = ?, joinType = ?, " + "minJoinLevel = ?, territory = ? WHERE id = ?";
    private static final String UPDATE_LEGION_DESCRIPTION_QUERY = "UPDATE legions SET description = ?, joinType = ?, minJoinLevel = ? WHERE id = ?";
    private static final String INSERT_ANNOUNCEMENT_QUERY = "INSERT INTO legion_announcement_list (`legion_id`, `announcement`, `date`) VALUES (?, ?, ?)";
    private static final String SELECT_ANNOUNCEMENTLIST_QUERY = "SELECT * FROM legion_announcement_list WHERE legion_id = ? ORDER BY date ASC LIMIT 7";
    private static final String DELETE_ANNOUNCEMENT_QUERY = "DELETE FROM legion_announcement_list WHERE legion_id = ? AND date = ?";
    private static final String INSERT_EMBLEM_QUERY = "INSERT INTO legion_emblems (legion_id, emblem_id, color_r, color_g, color_b, " + "emblem_type, emblem_data) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_EMBLEM_QUERY = "UPDATE legion_emblems SET emblem_id = ?, color_r = ?, color_g = ?, color_b = ?, " + "emblem_type = ?, emblem_data = ? WHERE legion_id = ?";
    private static final String SELECT_EMBLEM_QUERY = "SELECT * FROM legion_emblems WHERE legion_id = ?";
    private static final String SELECT_STORAGE_QUERY = "SELECT `item_unique_id`, `item_id`, `item_count`, `item_color`, `color_expires`, " + "`item_creator`, `expire_time`, `activation_count`, `is_equiped`, `slot`, `enchant`, " + "`enchant_bonus`, `item_skin`, `fusioned_item`, `optional_socket`, " + "`optional_fusion_socket`, `charge`, `rnd_bonus`, `rnd_count`, `wrappable_count`, " + "`is_packed`, `tempering_level`, `is_topped`, `strengthen_skill`, `skin_skill`, " + "`luna_reskin`, `reduction_level`, `is_seal`, `isEnhance`, `enhanceSkillId`, " + "`enhanceSkillEnchant` FROM `inventory` WHERE `item_owner` = ? AND `item_location` = ? AND `is_equiped` = ?";
    private static final String INSERT_HISTORY_QUERY = "INSERT INTO legion_history (`legion_id`, `date`, `history_type`, `name`, " + "`tab_id`, `description`) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_HISTORY_QUERY = "SELECT * FROM `legion_history` WHERE legion_id = ? ORDER BY date ASC";
    private static final String CLEAR_LEGION_SIEGE = "UPDATE siege_locations SET legion_id = 0 WHERE legion_id = ?";
    private static final String INSERT_RECRUIT_LIST_QUERY = "INSERT INTO legion_join_requests (`legionId`, `playerId`, `playerName`, " + "`playerClassId`, `playerRaceId`, `playerLevel`, `playerGenderId`, " + "`joinRequestMsg`, `date`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_RECRUIT_LIST_QUERY = "SELECT * FROM legion_join_requests WHERE legionId = ? ORDER BY date ASC";
    private static final String DELETE_RECRUIT_LIST_QUERY = "DELETE FROM legion_join_requests WHERE legionId = ? AND playerId = ?";
    private static final String SELECT_LEGION_IDS_WITH_TERRITORIES = "SELECT id FROM legions WHERE territory > 0";
    private static final String CHECK_NAME_USED_QUERY = "SELECT COUNT(id) as cnt FROM legions WHERE name = ?";
    private static final String SELECT_USED_IDS_QUERY = "SELECT id FROM legions";

    @Override
    public boolean isNameUsed(final String name) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(CHECK_NAME_USED_QUERY)) {
            
            s.setString(1, name);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            log.error("Can't check if legion name '{}' is used", name, e);
            return true;
        }
    }

    @Override
    public Collection<Integer> getLegionIdsWithTerritories() {
        Collection<Integer> legionIds = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(SELECT_LEGION_IDS_WITH_TERRITORIES);
             ResultSet rs = s.executeQuery()) {
            
            while (rs.next()) {
                legionIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            log.error("Error getting legions with territory", e);
        }
        
        return legionIds;
    }

    @Override
    public boolean saveNewLegion(final Legion legion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_LEGION_QUERY)) {
            
            log.debug("Saving new legion: {} {}", legion.getLegionId(), legion.getLegionName());
            
            stmt.setInt(1, legion.getLegionId());
            stmt.setString(2, legion.getLegionName());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error saving new legion: {}", legion.getLegionId(), e);
            return false;
        }
    }

    @Override
    public void storeLegion(final Legion legion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LEGION_QUERY)) {
            
            log.debug("Storing legion {} {}", legion.getLegionId(), legion.getLegionName());
            
            stmt.setString(1, legion.getLegionName());
            stmt.setInt(2, legion.getLegionLevel());
            stmt.setLong(3, legion.getContributionPoints());
            stmt.setInt(4, legion.getDeputyPermission());
            stmt.setInt(5, legion.getCenturionPermission());
            stmt.setInt(6, legion.getLegionaryPermission());
            stmt.setInt(7, legion.getVolunteerPermission());
            stmt.setInt(8, legion.getDisbandTime());
            stmt.setString(9, legion.getLegionDescription());
            stmt.setInt(10, legion.getLegionJoinType());
            stmt.setInt(11, legion.getMinLevel());
            
            int territoryId = (legion.getTerritory() != null && legion.getTerritory().getId() > 0) ? legion.getTerritory().getId() : 0;
            stmt.setInt(12, territoryId);
            stmt.setInt(13, legion.getLegionId());
            stmt.executeUpdate();
            
            // Store join requests
            if (!legion.getJoinRequestMap().isEmpty()) {
                for (LegionJoinRequest ljr : legion.getJoinRequestMap().values()) {
                    storeLegionJoinRequest(ljr);
                }
            }
        } catch (SQLException e) {
            log.error("Error storing legion: {}", legion.getLegionId(), e);
        }
    }

    @Override
    public Legion loadLegion(final String legionName) {
        Legion legion = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGION_QUERY2)) {
            
            stmt.setString(1, legionName);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    legion = createLegionFromResultSet(resultSet);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion by name: {}", legionName, e);
        }
        
        log.debug("Loaded legion: {}", legion != null ? legion.getLegionId() : "null");
        return legion;
    }

    @Override
    public Legion loadLegion(final int legionId) {
        Legion legion = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGION_QUERY1)) {
            
            stmt.setInt(1, legionId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    legion = createLegionFromResultSet(resultSet);
                    
                    // Load join requests
                    for (LegionJoinRequest ljr : loadLegionJoinRequests(legion.getLegionId())) {
                        legion.addJoinRequest(ljr);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion by id: {}", legionId, e);
        }
        
        log.debug("Loaded legion: {}", legion != null ? legion.getLegionId() : "null");
        return legion;
    }
    
    private Legion createLegionFromResultSet(ResultSet resultSet) throws SQLException {
        Legion legion = new Legion();
        legion.setLegionId(resultSet.getInt("id"));
        legion.setLegionName(resultSet.getString("name"));
        legion.setLegionLevel(resultSet.getInt("level"));
        legion.addContributionPoints(resultSet.getLong("contribution_points"));
        
        int terrId = resultSet.getInt("territory");
        LegionTerritory t = new LegionTerritory(terrId);
        if (terrId > 0) {
            t.setLegionId(legion.getLegionId());
            t.setLegionName(legion.getLegionName());
        }
        legion.setTerritory(t);
        
        legion.setLegionPermissions(
            resultSet.getShort("deputy_permission"),
            resultSet.getShort("centurion_permission"),
            resultSet.getShort("legionary_permission"),
            resultSet.getShort("volunteer_permission")
        );
        
        legion.setDescription(resultSet.getString("description"));
        legion.setJoinType(resultSet.getInt("joinType"));
        legion.setMinJoinLevel(resultSet.getInt("minJoinLevel"));
        legion.setDisbandTime(resultSet.getInt("disband_time"));
        
        return legion;
    }

    @Override
    public void deleteLegion(int legionId) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt1 = con.prepareStatement(DELETE_LEGION_QUERY);
                 PreparedStatement stmt2 = con.prepareStatement(CLEAR_LEGION_SIEGE)) {
                
                stmt1.setInt(1, legionId);
                stmt1.executeUpdate();
                
                stmt2.setInt(1, legionId);
                stmt2.executeUpdate();
                
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("Error deleting legion: {}", legionId, e);
        }
    }

    @Override
    public int[] getUsedIDs() {
        List<Integer> ids = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_USED_IDS_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            log.error("Can't get list of id's from legions table", e);
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    @Override
    public TreeMap<Timestamp, String> loadAnnouncementList(final int legionId) {
        final TreeMap<Timestamp, String> announcementList = new TreeMap<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ANNOUNCEMENTLIST_QUERY)) {
            
            stmt.setInt(1, legionId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    String message = resultSet.getString("announcement");
                    Timestamp date = resultSet.getTimestamp("date");
                    announcementList.put(date, message);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading announcement list for legion: {}", legionId, e);
        }

        log.debug("Loaded announcement list for legion: {}", legionId);
        return announcementList;
    }

    @Override
    public boolean saveNewAnnouncement(final int legionId, final Timestamp currentTime, final String message) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_ANNOUNCEMENT_QUERY)) {
            
            log.debug("Saving new announcement for legion: {}", legionId);
            
            stmt.setInt(1, legionId);
            stmt.setString(2, message);
            stmt.setTimestamp(3, currentTime);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error saving announcement for legion: {}", legionId, e);
            return false;
        }
    }

    @Override
    public void removeAnnouncement(int legionId, Timestamp unixTime) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ANNOUNCEMENT_QUERY)) {
            
            stmt.setInt(1, legionId);
            stmt.setTimestamp(2, unixTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error removing announcement for legion: {}", legionId, e);
        }
    }

    @Override
    public void storeLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
        if (!validEmblem(legionEmblem)) {
            return;
        }
        
        if (!checkEmblem(legionId)) {
            createLegionEmblem(legionId, legionEmblem);
        } else if (legionEmblem.getPersistentState() == PersistentState.UPDATE_REQUIRED) {
            updateLegionEmblem(legionId, legionEmblem);
        }
        
        legionEmblem.setPersistentState(PersistentState.UPDATED);
    }

    private boolean validEmblem(final LegionEmblem legionEmblem) {
        return !(legionEmblem.getEmblemType() == LegionEmblemType.CUSTOM && legionEmblem.getCustomEmblemData() == null);
    }

    private boolean checkEmblem(final int legionid) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(SELECT_EMBLEM_QUERY)) {
            
            st.setInt(1, legionid);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Can't check emblem for legion: {}", legionid, e);
            return false;
        }
    }

    private void createLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_EMBLEM_QUERY)) {
            
            stmt.setInt(1, legionId);
            stmt.setInt(2, legionEmblem.getEmblemId());
            stmt.setInt(3, legionEmblem.getColor_r());
            stmt.setInt(4, legionEmblem.getColor_g());
            stmt.setInt(5, legionEmblem.getColor_b());
            stmt.setString(6, legionEmblem.getEmblemType().toString());
            stmt.setBytes(7, legionEmblem.getCustomEmblemData());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error creating emblem for legion: {}", legionId, e);
        }
    }

    private void updateLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_EMBLEM_QUERY)) {
            
            stmt.setInt(1, legionEmblem.getEmblemId());
            stmt.setInt(2, legionEmblem.getColor_r());
            stmt.setInt(3, legionEmblem.getColor_g());
            stmt.setInt(4, legionEmblem.getColor_b());
            stmt.setString(5, legionEmblem.getEmblemType().toString());
            stmt.setBytes(6, legionEmblem.getCustomEmblemData());
            stmt.setInt(7, legionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating emblem for legion: {}", legionId, e);
        }
    }

    @Override
    public LegionEmblem loadLegionEmblem(final int legionId) {
        LegionEmblem emblem = new LegionEmblem();
        boolean found = false;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_EMBLEM_QUERY)) {
            
            stmt.setInt(1, legionId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    emblem.setEmblem(
                        resultSet.getInt("emblem_id"),
                        resultSet.getInt("color_r"),
                        resultSet.getInt("color_g"),
                        resultSet.getInt("color_b"),
                        LegionEmblemType.valueOf(resultSet.getString("emblem_type")),
                        resultSet.getBytes("emblem_data")
                    );
                    found = true;
                }
            }
        } catch (SQLException e) {
            log.error("Error loading emblem for legion: {}", legionId, e);
        }
        
        if (!found) {
            emblem.setEmblem(0, 0, 0, 0, LegionEmblemType.DEFAULT, null);
        }
        
        emblem.setPersistentState(PersistentState.UPDATED);
        return emblem;
    }

    @Override
    public LegionWarehouse loadLegionStorage(Legion legion) {
        final LegionWarehouse inventory = new LegionWarehouse(legion);
        final int legionId = legion.getLegionId();
        final int storage = StorageType.LEGION_WAREHOUSE.getId();
        final int equipped = 0;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_STORAGE_QUERY)) {
            
            stmt.setInt(1, legionId);
            stmt.setInt(2, storage);
            stmt.setInt(3, equipped);
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    Item item = constructItem(rset, storage);
                    item.setPersistentState(PersistentState.UPDATED);
                    inventory.onLoadHandler(item);
                }
            }
        } catch (Exception e) {
            log.error("Could not restore legion storage data for legion: {}", legionId, e);
        }
        
        return inventory;
    }
    
    private Item constructItem(ResultSet rset, int storage) throws SQLException {
        int itemUniqueId = rset.getInt("item_unique_id");
        int itemId = rset.getInt("item_id");
        long itemCount = rset.getLong("item_count");
        int itemColor = rset.getInt("item_color");
        int colorExpireTime = rset.getInt("color_expires");
        String itemCreator = rset.getString("item_creator");
        int expireTime = rset.getInt("expire_time");
        int activationCount = rset.getInt("activation_count");
        int isEquiped = rset.getInt("is_equiped");
        int slot = rset.getInt("slot");
        int enchant = rset.getInt("enchant");
        int enchantBonus = rset.getInt("enchant_bonus");
        int itemSkin = rset.getInt("item_skin");
        int fusionedItem = rset.getInt("fusioned_item");
        int optionalSocket = rset.getInt("optional_socket");
        int optionalFusionSocket = rset.getInt("optional_fusion_socket");
        int charge = rset.getInt("charge");
        Integer randomNumber = rset.getInt("rnd_bonus");
        int rndCount = rset.getInt("rnd_count");
        int wrappingCount = rset.getInt("wrappable_count");
        int isPacked = rset.getInt("is_packed");
        int temperingLevel = rset.getInt("tempering_level");
        int isTopped = rset.getInt("is_topped");
        int strengthenSkill = rset.getInt("strengthen_skill");
        int skinSkill = rset.getInt("skin_skill");
        int isLunaReskin = rset.getInt("luna_reskin");
        int reductionLevel = rset.getInt("reduction_level");
        int unSeal = rset.getInt("is_seal");
        boolean isEnhance = rset.getBoolean("isEnhance");
        int enhanceSkillId = rset.getInt("enhanceSkillId");
        int enhanceSkillEnchant = rset.getInt("enhanceSkillEnchant");
        
        return new Item(itemUniqueId, itemId, itemCount, itemColor, colorExpireTime, itemCreator, expireTime, activationCount, isEquiped == 1, false, slot, storage, enchant, enchantBonus, itemSkin, fusionedItem, optionalSocket, optionalFusionSocket, charge, randomNumber, rndCount, wrappingCount, isPacked == 1, temperingLevel, isTopped == 1, strengthenSkill, skinSkill, isLunaReskin == 1, reductionLevel, unSeal, isEnhance, enhanceSkillId, enhanceSkillEnchant);
    }

    @Override
    public void loadLegionHistory(final Legion legion) {
        final Collection<LegionHistory> history = legion.getLegionHistory();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_HISTORY_QUERY)) {
            
            stmt.setInt(1, legion.getLegionId());
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    try {
                        history.add(new LegionHistory(
                            LegionHistoryType.valueOf(resultSet.getString("history_type")),
                            resultSet.getString("name"),
                            resultSet.getTimestamp("date"),
                            resultSet.getInt("tab_id"),
                            resultSet.getString("description")
                        ));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid history type for legion {}: {}", legion.getLegionId(), resultSet.getString("history_type"));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion history for legion: {}", legion.getLegionId(), e);
        }
    }

    @Override
    public boolean saveNewLegionHistory(final int legionId, final LegionHistory legionHistory) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_HISTORY_QUERY)) {
            
            stmt.setInt(1, legionId);
            stmt.setTimestamp(2, legionHistory.getTime());
            stmt.setString(3, legionHistory.getLegionHistoryType().toString());
            stmt.setString(4, legionHistory.getName());
            stmt.setInt(5, legionHistory.getTabId());
            stmt.setString(6, legionHistory.getDescription());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error saving legion history for legion: {}", legionId, e);
            return false;
        }
    }

    @Override
    public void updateLegionDescription(final Legion legion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LEGION_DESCRIPTION_QUERY)) {
            
            stmt.setString(1, legion.getLegionDescription());
            stmt.setInt(2, legion.getLegionJoinType());
            stmt.setInt(3, legion.getMinLevel());
            stmt.setInt(4, legion.getLegionId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating legion description for legion: {}", legion.getLegionId(), e);
        }
    }

    @Override
    public void storeLegionJoinRequest(final LegionJoinRequest legionJoinRequest) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_RECRUIT_LIST_QUERY)) {
            
            stmt.setInt(1, legionJoinRequest.getLegionId());
            stmt.setInt(2, legionJoinRequest.getPlayerId());
            stmt.setString(3, legionJoinRequest.getPlayerName());
            stmt.setInt(4, legionJoinRequest.getPlayerClass());
            stmt.setInt(5, legionJoinRequest.getRace());
            stmt.setInt(6, legionJoinRequest.getLevel());
            stmt.setInt(7, legionJoinRequest.getGenderId());
            stmt.setString(8, legionJoinRequest.getMsg());
            stmt.setTimestamp(9, legionJoinRequest.getDate());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error storing legion join request for legion: {} player: {}", legionJoinRequest.getLegionId(), legionJoinRequest.getPlayerId(), e);
        }
    }
    
    @Override
    public FastList<LegionJoinRequest> loadLegionJoinRequests(final int legionId) {
        final FastList<LegionJoinRequest> requestList = new FastList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_RECRUIT_LIST_QUERY)) {
            
            stmt.setInt(1, legionId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    LegionJoinRequest ljr = new LegionJoinRequest();
                    ljr.setLegionId(resultSet.getInt("legionId"));
                    ljr.setPlayerId(resultSet.getInt("playerId"));
                    ljr.setPlayerName(resultSet.getString("playerName"));
                    ljr.setPlayerClass(resultSet.getInt("playerClassId"));
                    ljr.setRace(resultSet.getInt("playerRaceId"));
                    ljr.setLevel(resultSet.getInt("playerLevel"));
                    ljr.setGenderId(resultSet.getInt("playerGenderId"));
                    ljr.setDate(resultSet.getTimestamp("date"));
                    requestList.add(ljr);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading legion join requests for legion: {}", legionId, e);
        }
        
        return requestList;
    }
    
    @Override
    public void deleteLegionJoinRequest(int legionId, int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_RECRUIT_LIST_QUERY)) {
            
            stmt.setInt(1, legionId);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting legion join request for legion {} player {}", legionId, playerId, e);
        }
    }
    
    @Override
    public void deleteLegionJoinRequest(LegionJoinRequest ljr) {
        deleteLegionJoinRequest(ljr.getLegionId(), ljr.getPlayerId());
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}