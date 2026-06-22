package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseRegistry;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.housing.PartType;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.item.HouseObjectFactory;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import javolution.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * MySQL 8 implementation of PlayerRegisteredItemsDAO
 * Fixed connection leaks
 */
public class MySQL8PlayerRegisteredItemsDAO extends PlayerRegisteredItemsDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerRegisteredItemsDAO.class);
    
    public static final String CLEAN_PLAYER_QUERY = "DELETE FROM `player_registered_items` WHERE `player_id` = ?";
    
    public static final String SELECT_QUERY = "SELECT * FROM `player_registered_items` WHERE `player_id` = ?";
    
    public static final String INSERT_QUERY = "INSERT INTO `player_registered_items` " + "(`expire_time`, `color`, `color_expires`, `owner_use_count`, `visitor_use_count`, " + "`x`, `y`, `z`, `h`, `area`, `floor`, `player_id`, `item_unique_id`, `item_id`) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    public static final String UPDATE_QUERY = "UPDATE `player_registered_items` SET " + "`expire_time` = ?, `color` = ?, `color_expires` = ?, `owner_use_count` = ?, " + "`visitor_use_count` = ?, `x` = ?, `y` = ?, `z` = ?, `h` = ?, `area` = ?, `floor` = ? " + "WHERE `player_id` = ? AND `item_unique_id` = ? AND `item_id` = ?";
    
    public static final String DELETE_QUERY = "DELETE FROM `player_registered_items` WHERE `item_unique_id` = ?";
    
    public static final String RESET_QUERY = "UPDATE `player_registered_items` SET x = 0, y = 0, z = 0, h = 0, area = 'NONE' " + "WHERE `player_id` = ? AND `area` != 'DECOR'";
    
    public static final String SELECT_USED_IDS_QUERY = "SELECT item_unique_id FROM player_registered_items WHERE item_unique_id <> 0";

    @Override
    public int[] getUsedIDs() {
        List<Integer> ids = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_USED_IDS_QUERY, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            log.error("Can't get list of id's from player_registered_items table", e);
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }
    
    @Override
    public void loadRegistry(int playerId) {
        House house = HousingService.getInstance().getPlayerStudio(playerId);
        if (house == null) {
            int address = HousingService.getInstance().getPlayerAddress(playerId);
            house = HousingService.getInstance().getHouseByAddress(address);
        }
        
        if (house == null) {
            log.warn("No house found for player: {}", playerId);
            return;
        }
        
        HouseRegistry registry = house.getRegistry();
        
        try (Connection con = DatabaseFactory.getConnection(); 
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                HashMap<PartType, List<HouseDecoration>> usedParts = new HashMap<>();
                
                while (rset.next()) {
                    String area = rset.getString("area");
                    if ("DECOR".equals(area)) {
                        HouseDecoration dec = createDecoration(rset);
                        registry.putCustomPart(dec);
                        
                        if (dec.isUsed()) {
                            if (house.getHouseType() != HouseType.PALACE && dec.getFloor() > 0) {
                                dec.setFloor(0);
                            }
                            
                            List<HouseDecoration> usedForType = usedParts.computeIfAbsent(dec.getTemplate().getType(), k -> new ArrayList<>());
                            usedForType.add(dec);
                        }
                        
                        dec.setPersistentState(PersistentState.UPDATED);
                    } else {
                        HouseObject<?> obj = constructObject(registry, house, rset);
                        registry.putObject(obj);
                        obj.setPersistentState(PersistentState.UPDATED);
                    }
                }
                
                for (PartType partType : PartType.values()) {
                    if (usedParts.containsKey(partType)) {
                        for (HouseDecoration usedDeco : usedParts.get(partType)) {
                            registry.setPartInUse(usedDeco, usedDeco.getFloor());
                        }
                        continue;
                    }
                    
                    int floorCount = 1;
                    if (house.getHouseType() == HouseType.PALACE && 
                        (partType == PartType.INFLOOR_ANY || partType == PartType.INWALL_ANY)) {
                        floorCount = 6;
                    }
                    
                    for (int i = 0; i < floorCount; i++) {
                        HouseDecoration def = registry.getDefaultPartByType(partType, i);
                        if (def != null) {
                            registry.setPartInUse(def, i);
                        }
                    }
                }
                
                registry.setPersistentState(PersistentState.UPDATED);
            }
        } catch (Exception e) {
            log.error("Could not restore house registry data for player: {}", playerId, e);
        }
    }
    
    private HouseObject<?> constructObject(final HouseRegistry registry, House house, ResultSet rset) throws SQLException {
        int itemUniqueId = rset.getInt("item_unique_id");
        VisibleObject visObj = World.getInstance().findVisibleObject(itemUniqueId);
        HouseObject<?> obj = null;
        
        if (visObj != null) {
            if (visObj instanceof HouseObject<?>) {
                obj = (HouseObject<?>) visObj;
            } else {
                throw new IllegalAccessError("Invalid object type for house object id: " + itemUniqueId);
            }
        } else {
            obj = registry.getObjectByObjId(itemUniqueId);
            if (obj == null) {
                obj = HouseObjectFactory.createNew(house, itemUniqueId, rset.getInt("item_id"));
            }
        }
        
        obj.setOwnerUsedCount(rset.getInt("owner_use_count"));
        obj.setVisitorUsedCount(rset.getInt("visitor_use_count"));
        obj.setX(rset.getFloat("x"));
        obj.setY(rset.getFloat("y"));
        obj.setZ(rset.getFloat("z"));
        obj.setHeading((byte) rset.getInt("h"));
        obj.setColor(rset.getInt("color"));
        obj.setColorExpireEnd(rset.getInt("color_expires"));
        
        if (obj.getObjectTemplate().getUseDays() > 0) {
            obj.setExpireTime(rset.getInt("expire_time"));
        }
        
        return obj;
    }
    
    private HouseDecoration createDecoration(ResultSet rset) throws SQLException {
        int itemUniqueId = rset.getInt("item_unique_id");
        int itemId = rset.getInt("item_Id");
        byte floor = rset.getByte("floor");
        HouseDecoration decor = new HouseDecoration(itemUniqueId, itemId, floor);
        decor.setUsed(rset.getInt("owner_use_count") > 0);
        return decor;
    }
    
    @Override
    public boolean store(HouseRegistry registry, int playerId) {
        FastList<HouseObject<?>> objects = registry.getObjects();
        FastList<HouseDecoration> decors = registry.getAllParts();
        
        Collection<HouseObject<?>> objectsToAdd = new ArrayList<>();
        Collection<HouseObject<?>> objectsToUpdate = new ArrayList<>();
        Collection<HouseObject<?>> objectsToDelete = new ArrayList<>();
        
        Collection<HouseDecoration> partsToAdd = new ArrayList<>();
        Collection<HouseDecoration> partsToUpdate = new ArrayList<>();
        Collection<HouseDecoration> partsToDelete = new ArrayList<>();
        
        // Filter objects
        for (HouseObject<?> obj : objects) {
            if (obj != null) {
                PersistentState state = obj.getPersistentState();
                if (state == PersistentState.NEW) {
                    objectsToAdd.add(obj);
                } else if (state == PersistentState.UPDATE_REQUIRED) {
                    objectsToUpdate.add(obj);
                } else if (state == PersistentState.DELETED) {
                    objectsToDelete.add(obj);
                }
            }
        }
        
        // Filter decorations
        for (HouseDecoration dec : decors) {
            if (dec != null) {
                PersistentState state = dec.getPersistentState();
                if (state == PersistentState.NEW) {
                    partsToAdd.add(dec);
                } else if (state == PersistentState.UPDATE_REQUIRED) {
                    partsToUpdate.add(dec);
                } else if (state == PersistentState.DELETED) {
                    partsToDelete.add(dec);
                }
            }
        }
        
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            deleteObjects(con, objectsToDelete);
            deleteParts(con, partsToDelete);
            storeObjects(con, objectsToUpdate, playerId, false);
            storeParts(con, partsToUpdate, playerId, false);
            storeObjects(con, objectsToAdd, playerId, true);
            storeParts(con, partsToAdd, playerId, true);
            
            con.commit();
            registry.setPersistentState(PersistentState.UPDATED);
            
        } catch (SQLException e) {
            log.error("Can't save player registered items: {}", playerId, e);
            try (Connection con = DatabaseFactory.getConnection()) {
                con.rollback();
            } catch (SQLException rollbackEx) {
                log.error("Failed to rollback registered items transaction for player {}", playerId, rollbackEx);
            }
            return false;
        }
        
        // Update states
        for (HouseObject<?> obj : objects) {
            if (obj != null) {
                if (obj.getPersistentState() == PersistentState.DELETED) {
                    registry.discardObject(obj.getObjectId());
                } else {
                    obj.setPersistentState(PersistentState.UPDATED);
                }
            }
        }
        
        for (HouseDecoration decor : decors) {
            if (decor != null) {
                if (decor.getPersistentState() == PersistentState.DELETED) {
                    registry.discardPart(decor);
                } else {
                    decor.setPersistentState(PersistentState.UPDATED);
                }
            }
        }
        
        // Release IDs
        if (!objectsToDelete.isEmpty()) {
            for (HouseObject<?> obj : objectsToDelete) {
                if (obj != null && obj.getObjectId() != 0) {
                    IDFactory.getInstance().releaseId(obj.getObjectId());
                }
            }
        }
        
        if (!partsToDelete.isEmpty()) {
            for (HouseDecoration part : partsToDelete) {
                if (part != null && part.getObjectId() != 0) {
                    IDFactory.getInstance().releaseId(part.getObjectId());
                }
            }
        }
        
        return true;
    }
    
    private boolean storeObjects(Connection con, Collection<HouseObject<?>> objects, int playerId, boolean isNew) throws SQLException {
        if (GenericValidator.isBlankOrNull(objects)) {
            return true;
        }
        
        String query = isNew ? INSERT_QUERY : UPDATE_QUERY;
        
        try (PreparedStatement stmt = con.prepareStatement(query)) {
            for (HouseObject<?> obj : objects) {
                if (obj == null) continue;
                
                if (obj.getExpireTime() > 0) {
                    stmt.setInt(1, obj.getExpireTime());
                } else {
                    stmt.setNull(1, Types.INTEGER);
                }
                
                if (obj.getColor() == null) {
                    stmt.setNull(2, Types.INTEGER);
                } else {
                    stmt.setInt(2, obj.getColor());
                }
                
                stmt.setInt(3, obj.getColorExpireEnd());
                stmt.setInt(4, obj.getOwnerUsedCount());
                stmt.setInt(5, obj.getVisitorUsedCount());
                stmt.setFloat(6, obj.getX());
                stmt.setFloat(7, obj.getY());
                stmt.setFloat(8, obj.getZ());
                stmt.setInt(9, obj.getHeading());
                
                if (obj.getX() > 0 || obj.getY() > 0 || obj.getZ() > 0) {
                    stmt.setString(10, obj.getPlaceArea().toString());
                } else {
                    stmt.setString(10, "NONE");
                }
                
                stmt.setByte(11, (byte) 0);
                stmt.setInt(12, playerId);
                stmt.setInt(13, obj.getObjectId());
                stmt.setInt(14, obj.getObjectTemplate().getTemplateId());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            return true;
        }
    }
    
    private boolean storeParts(Connection con, Collection<HouseDecoration> parts, int playerId, boolean isNew) throws SQLException {
        if (GenericValidator.isBlankOrNull(parts)) {
            return true;
        }
        
        String query = isNew ? INSERT_QUERY : UPDATE_QUERY;
        
        try (PreparedStatement stmt = con.prepareStatement(query)) {
            for (HouseDecoration part : parts) {
                if (part == null) continue;
                
                stmt.setNull(1, Types.INTEGER);
                stmt.setNull(2, Types.INTEGER);
                stmt.setInt(3, 0);
                stmt.setInt(4, part.isUsed() ? 1 : 0);
                stmt.setInt(5, 0);
                stmt.setFloat(6, 0);
                stmt.setFloat(7, 0);
                stmt.setFloat(8, 0);
                stmt.setInt(9, 0);
                stmt.setString(10, "DECOR");
                stmt.setByte(11, part.getFloor());
                stmt.setInt(12, playerId);
                stmt.setInt(13, part.getObjectId());
                stmt.setInt(14, part.getTemplate().getId());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            return true;
        }
    }
    
    private boolean deleteObjects(Connection con, Collection<HouseObject<?>> objects) throws SQLException {
        if (GenericValidator.isBlankOrNull(objects)) {
            return true;
        }
        
        try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            for (HouseObject<?> obj : objects) {
                if (obj == null) continue;
                stmt.setInt(1, obj.getObjectId());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            return true;
        }
    }
    
    private boolean deleteParts(Connection con, Collection<HouseDecoration> parts) throws SQLException {
        if (GenericValidator.isBlankOrNull(parts)) {
            return true;
        }
        
        try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            for (HouseDecoration part : parts) {
                if (part == null) continue;
                stmt.setInt(1, part.getObjectId());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            return true;
        }
    }
    
    @Override
    public boolean deletePlayerItems(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CLEAN_PLAYER_QUERY)) {
            
            log.info("Deleting player registered items for player: {}", playerId);
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error("Error deleting all player registered items. PlayerObjId: {}", playerId, e);
            return false;
        }
    }
    
    @Override
    public void resetRegistry(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(RESET_QUERY)) {
            
            log.info("Resetting player registered items for player: {}", playerId);
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error resetting player registered items. PlayerObjId: {}", playerId, e);
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}