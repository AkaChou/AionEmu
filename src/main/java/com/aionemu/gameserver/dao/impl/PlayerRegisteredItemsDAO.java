package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.*;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseRegistry;
import com.aionemu.gameserver.model.templates.housing.HouseType;
import com.aionemu.gameserver.model.templates.housing.PartType;
import com.aionemu.gameserver.services.item.HouseObjectFactory;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * 玩家已注册房屋物品数据访问对象的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerRegisteredItemsDAO.
 */
@Slf4j
public class PlayerRegisteredItemsDAO extends com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO {

    /** 清理玩家全部注册物品 / Delete all registered items for a player */
    public static final String CLEAN_PLAYER_QUERY = "DELETE FROM `player_registered_items` WHERE `player_id` = ?";

    /** 加载玩家注册物品 / Select all registered items for a player */
    public static final String SELECT_QUERY = "SELECT * FROM `player_registered_items` WHERE `player_id` = ?";

    /** 插入注册物品 / Insert a house registered item or decoration */
    public static final String INSERT_QUERY = "INSERT INTO `player_registered_items` " + "(`expire_time`, `color`, `color_expires`, `owner_use_count`, `visitor_use_count`, " + "`x`, `y`, `z`, `h`, `area`, `floor`, `player_id`, `item_unique_id`, `item_id`) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 更新注册物品 / Update a house registered item or decoration */
    public static final String UPDATE_QUERY = "UPDATE `player_registered_items` SET " + "`expire_time` = ?, `color` = ?, `color_expires` = ?, `owner_use_count` = ?, " + "`visitor_use_count` = ?, `x` = ?, `y` = ?, `z` = ?, `h` = ?, `area` = ?, `floor` = ? " + "WHERE `player_id` = ? AND `item_unique_id` = ? AND `item_id` = ?";

    /** 按唯一 ID 删除注册物品 / Delete a registered item by unique id */
    public static final String DELETE_QUERY = "DELETE FROM `player_registered_items` WHERE `item_unique_id` = ?";

    /** 重置非装饰物品摆放位置 / Reset placement of non-decoration registered items */
    public static final String RESET_QUERY = "UPDATE `player_registered_items` SET x = 0, y = 0, z = 0, h = 0, area = 'NONE' " + "WHERE `player_id` = ? AND `area` != 'DECOR'";

    /** 查询已占用的唯一 ID / Select used item unique ids */
    public static final String SELECT_USED_IDS_QUERY = "SELECT item_unique_id FROM player_registered_items WHERE item_unique_id <> 0";

    /**
     * 获取已占用的物品唯一 ID 列表，供 ID 工厂回收/分配使用。
     * Returns used item unique ids for ID factory allocation.
     *
     * 已占用 ID 数组；出错时返回空数组。
     * used id array, or empty on error.
     */
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
            log.error(I18n.get("log.0bebcd82021c", e));
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }
    
    /**
     * 加载玩家房屋注册物品与装饰部件到房屋注册表。
     * Loads the player's house registered items and decorations into the registry.
     *
     * player object id
     */
    @Override
    public void loadRegistry(int playerId) {
        House house = GameHousingServices.housingService().getPlayerStudio(playerId);
        if (house == null) {
            int address = GameHousingServices.housingService().getPlayerAddress(playerId);
            house = GameHousingServices.housingService().getHouseByAddress(address);
        }
        
        if (house == null) {
            log.warn(I18n.get("log.17502383de83", playerId));
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
            log.error(I18n.get("log.98d5b9d41325", playerId, e));
        }
    }
    
    private HouseObject<?> constructObject(final HouseRegistry registry, House house, ResultSet rset) throws SQLException {
        int itemUniqueId = rset.getInt("item_unique_id");
        VisibleObject visObj = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(itemUniqueId);
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
    
    /**
     * 按持久化状态批量保存房屋注册表（增删改），事务提交后更新内存状态并释放已删 ID。
     * Persists the house registry by persistent state (insert/update/delete), then updates in-memory state and releases deleted ids.
     *
     * @param registry 房屋注册表 / house registry
     * player object id
     *
     * @return 是否保存成功 / whether store succeeded
     */
    @Override
    public boolean store(HouseRegistry registry, int playerId) {
        List<HouseObject<?>> objects = registry.getObjects();
        List<HouseDecoration> decors = registry.getAllParts();
        
        Collection<HouseObject<?>> objectsToAdd = new ArrayList<>();
        Collection<HouseObject<?>> objectsToUpdate = new ArrayList<>();
        Collection<HouseObject<?>> objectsToDelete = new ArrayList<>();
        
        Collection<HouseDecoration> partsToAdd = new ArrayList<>();
        Collection<HouseDecoration> partsToUpdate = new ArrayList<>();
        Collection<HouseDecoration> partsToDelete = new ArrayList<>();
        
        // 过滤对象 / Filter objects
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
        
        // 过滤装饰 / Filter decorations
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
			try {
				deleteObjects(con, objectsToDelete);
				deleteParts(con, partsToDelete);
				storeObjects(con, objectsToUpdate, playerId, false);
				storeParts(con, partsToUpdate, playerId, false);
				storeObjects(con, objectsToAdd, playerId, true);
				storeParts(con, partsToAdd, playerId, true);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
			registry.setPersistentState(PersistentState.UPDATED);
		} catch (SQLException e) {
			log.error(I18n.get("log.7c25eacf0aac", playerId, e));
			return false;
        }
        
        // 更新状态 / Update states
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
        
        // 释放 ID / Release IDs
        if (!objectsToDelete.isEmpty()) {
            for (HouseObject<?> obj : objectsToDelete) {
                if (obj != null && obj.getObjectId() != 0) {
                    GameWorldBootstrapServices.idFactory().releaseId(obj.getObjectId());
                }
            }
        }
        
        if (!partsToDelete.isEmpty()) {
            for (HouseDecoration part : partsToDelete) {
                if (part != null && part.getObjectId() != 0) {
                    GameWorldBootstrapServices.idFactory().releaseId(part.getObjectId());
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
    
    /**
     * 删除指定玩家的全部房屋注册物品。
     * Deletes all house registered items for the given player.
     *
     * player object id
     *
     * @param playerId
     * @return 是否删除成功 / whether delete succeeded
     */
    @Override
    public boolean deletePlayerItems(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CLEAN_PLAYER_QUERY)) {
            
            log.info(I18n.get("log.a62e9090f3a6", playerId));
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error(I18n.get("log.65741ede13d8", playerId, e));
            return false;
        }
    }
    
    /**
     * 重置玩家非装饰注册物品的摆放坐标与区域。
     * Resets placement coordinates and area for the player's non-decoration registered items.
     *
     * player object id
     */
    @Override
    public void resetRegistry(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(RESET_QUERY)) {
            
            log.info(I18n.get("log.5c21c4ee04a7", playerId));
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error(I18n.get("log.7f671b6e18ee", playerId, e));
        }
    }
    
    /**
     * 判断当前数据库是否受本 DAO 支持（MySQL 8）。
     * Checks whether the given database is supported by this DAO (MySQL 8).
     *
     * @param databaseName 数据库产品名 / database product name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
