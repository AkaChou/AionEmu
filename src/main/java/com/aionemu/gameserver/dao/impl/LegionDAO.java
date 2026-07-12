package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * 军团数据访问对象的 MySQL 8 实现，已修复连接泄漏。
 * MySQL 8 implementation of LegionDAO with connection leak fixes.
 *
 * @author Simple
 * @modified cura
 */
@Slf4j
public class LegionDAO extends com.aionemu.gameserver.dao.LegionDAO {

    /** 插入新军团 / Insert a new legion row */
    private static final String INSERT_LEGION_QUERY = "INSERT INTO legions (id, `name`) VALUES (?, ?)";
    /** 按 ID 查询军团 / Select legion by id */
    private static final String SELECT_LEGION_QUERY1 = "SELECT * FROM legions WHERE id = ?";
    /** 按名称查询军团 / Select legion by name */
    private static final String SELECT_LEGION_QUERY2 = "SELECT * FROM legions WHERE name = ?";
    /** 删除军团 / Delete legion by id */
    private static final String DELETE_LEGION_QUERY = "DELETE FROM legions WHERE id = ?";
    /** 更新军团完整信息 / Update full legion row */
    private static final String UPDATE_LEGION_QUERY = "UPDATE legions SET name = ?, level = ?, contribution_points = ?, " + "deputy_permission = ?, centurion_permission = ?, legionary_permission = ?, " + "volunteer_permission = ?, disband_time = ?, description = ?, joinType = ?, " + "minJoinLevel = ?, territory = ? WHERE id = ?";
    /** 更新军团描述与加入条件 / Update legion description and join settings */
    private static final String UPDATE_LEGION_DESCRIPTION_QUERY = "UPDATE legions SET description = ?, joinType = ?, minJoinLevel = ? WHERE id = ?";
    /** 插入军团公告 / Insert legion announcement */
    private static final String INSERT_ANNOUNCEMENT_QUERY = "INSERT INTO legion_announcement_list (`legion_id`, `announcement`, `date`) VALUES (?, ?, ?)";
    /** 加载军团公告列表 / Select legion announcements (latest 7) */
    private static final String SELECT_ANNOUNCEMENTLIST_QUERY = "SELECT * FROM legion_announcement_list WHERE legion_id = ? ORDER BY date ASC LIMIT 7";
    /** 删除军团公告 / Delete legion announcement by date */
    private static final String DELETE_ANNOUNCEMENT_QUERY = "DELETE FROM legion_announcement_list WHERE legion_id = ? AND date = ?";
    /** 插入军团徽章 / Insert legion emblem */
    private static final String INSERT_EMBLEM_QUERY = "INSERT INTO legion_emblems (legion_id, emblem_id, color_r, color_g, color_b, " + "emblem_type, emblem_data) VALUES (?, ?, ?, ?, ?, ?, ?)";
    /** 更新军团徽章 / Update legion emblem */
    private static final String UPDATE_EMBLEM_QUERY = "UPDATE legion_emblems SET emblem_id = ?, color_r = ?, color_g = ?, color_b = ?, " + "emblem_type = ?, emblem_data = ? WHERE legion_id = ?";
    /** 查询军团徽章 / Select legion emblem */
    private static final String SELECT_EMBLEM_QUERY = "SELECT * FROM legion_emblems WHERE legion_id = ?";
    /** 查询军团仓库物品 / Select legion warehouse items */
    private static final String SELECT_STORAGE_QUERY = "SELECT `item_unique_id`, `item_id`, `item_count`, `item_color`, `color_expires`, " + "`item_creator`, `expire_time`, `activation_count`, `is_equiped`, `slot`, `enchant`, " + "`enchant_bonus`, `item_skin`, `fusioned_item`, `optional_socket`, " + "`optional_fusion_socket`, `charge`, `rnd_bonus`, `rnd_count`, `wrappable_count`, " + "`is_packed`, `tempering_level`, `is_topped`, `strengthen_skill`, `skin_skill`, " + "`luna_reskin`, `reduction_level`, `is_seal`, `isEnhance`, `enhanceSkillId`, " + "`enhanceSkillEnchant` FROM `inventory` WHERE `item_owner` = ? AND `item_location` = ? AND `is_equiped` = ?";
    /** 插入军团历史 / Insert legion history entry */
    private static final String INSERT_HISTORY_QUERY = "INSERT INTO legion_history (`legion_id`, `date`, `history_type`, `name`, " + "`tab_id`, `description`) VALUES (?, ?, ?, ?, ?, ?)";
    /** 查询军团历史 / Select legion history */
    private static final String SELECT_HISTORY_QUERY = "SELECT * FROM `legion_history` WHERE legion_id = ? ORDER BY date ASC";
    /** 清除军团攻城归属 / Clear legion siege ownership */
    private static final String CLEAR_LEGION_SIEGE = "UPDATE siege_locations SET legion_id = 0 WHERE legion_id = ?";
    /** 插入军团加入申请 / Insert legion join request */
    private static final String INSERT_RECRUIT_LIST_QUERY = "INSERT INTO legion_join_requests (`legionId`, `playerId`, `playerName`, " + "`playerClassId`, `playerRaceId`, `playerLevel`, `playerGenderId`, " + "`joinRequestMsg`, `date`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    /** 查询军团加入申请 / Select legion join requests */
    private static final String SELECT_RECRUIT_LIST_QUERY = "SELECT * FROM legion_join_requests WHERE legionId = ? ORDER BY date ASC";
    /** 删除军团加入申请 / Delete legion join request */
    private static final String DELETE_RECRUIT_LIST_QUERY = "DELETE FROM legion_join_requests WHERE legionId = ? AND playerId = ?";
    /** 查询拥有领地的军团 ID / Select legion ids that own territories */
    private static final String SELECT_LEGION_IDS_WITH_TERRITORIES = "SELECT id FROM legions WHERE territory > 0";
    /** 检查军团名是否已占用 / Count legions with the given name */
    private static final String CHECK_NAME_USED_QUERY = "SELECT COUNT(id) as cnt FROM legions WHERE name = ?";
    /** 查询已占用的军团 ID / Select used legion ids */
    private static final String SELECT_USED_IDS_QUERY = "SELECT id FROM legions";

    /**
     * 检查军团名称是否已被占用。
     * Checks whether the given legion name is already used.
     *
     * @param name 军团名称 / legion name
     * @return 是否已占用；查询失败时返回 true / whether used; true on query error
     */
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
            log.error(I18n.get("log.84ada47a4666", name, e));
            return true;
        }
    }

    /**
     * 获取拥有领地（territory &gt; 0）的军团 ID 集合。
     * Returns legion ids that currently own a territory (territory &gt; 0).
     *
     * collection of legion ids
     */
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
            log.error(I18n.get("log.3117e02da50f", e));
        }

        return legionIds;
    }

    /**
     * 保存新建军团的基础记录（ID 与名称）。
     * Saves a newly created legion base row (id and name).
     *
     * legion
     * whether succeeded
     */
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
            log.error(I18n.get("log.5560e48eed9e", legion.getLegionId(), e));
            return false;
        }
    }

    /**
     * 持久化军团完整信息，并写入待处理的加入申请。
     * Persists full legion data and stores pending join requests.
     *
     * legion
     */
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

            // 存储加入请求 / Store join requests
            if (!legion.getJoinRequestMap().isEmpty()) {
                for (LegionJoinRequest ljr : legion.getJoinRequestMap().values()) {
                    storeLegionJoinRequest(ljr);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.9f74efb13ee5", legion.getLegionId(), e));
        }
    }

    /**
     * 按名称加载军团。
     * Loads a legion by name.
     *
     * legion name
     *
     * @param legionName @return 军团；不存在时返回 null / legion, or null if missing
     */
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
            log.error(I18n.get("log.ef450357e2bb", legionName, e));
        }

        log.debug("Loaded legion: {}", legion != null ? legion.getLegionId() : "null");
        return legion;
    }

    /**
     * 按 ID 加载军团，并附加加入申请列表。
     * Loads a legion by id and attaches join requests.
     *
     * legion id
     *
     * @param legionId @return 军团；不存在时返回 null / legion, or null if missing
     */
    @Override
    public Legion loadLegion(final int legionId) {
        Legion legion = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGION_QUERY1)) {

            stmt.setInt(1, legionId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    legion = createLegionFromResultSet(resultSet);

                    // 加载加入请求 / Load join requests
                    for (LegionJoinRequest ljr : loadLegionJoinRequests(legion.getLegionId())) {
                        legion.addJoinRequest(ljr);
                    }
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.bd9d366b0d7a", legionId, e));
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

    /**
     * 删除军团并清除其攻城归属（事务）。
     * Deletes the legion and clears its siege ownership within a transaction.
     *
     * legion id
     */
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
            log.error(I18n.get("log.9be4d3aa4400", legionId, e));
        }
    }

    /**
     * 获取已占用的军团 ID 列表，供 ID 工厂使用。
     * Returns used legion ids for ID factory allocation.
     *
     * 已占用 ID 数组；出错时返回空数组。
     * used id array, or empty on error.
     */
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
            log.error(I18n.get("log.82d77c44202e", e));
            return new int[0];
        }

        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    /**
     * 加载军团公告列表（最多 7 条，按时间升序）。
     * Loads legion announcements (up to 7, ordered by date ascending).
     *
     * legion id
     *
     * @param legionId @return 时间戳到公告内容的有序映射 / ordered map of timestamp to message
     */
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
            log.error(I18n.get("log.c4ec74ecbd88", legionId, e));
        }

        log.debug("Loaded announcement list for legion: {}", legionId);
        return announcementList;
    }

    /**
     * 保存一条新的军团公告。
     * Saves a new legion announcement.
     *
     * legion id
     * announcement timestamp
     * announcement message
     * whether succeeded
     */
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
            log.error(I18n.get("log.d7d4b79baa01", legionId, e));
            return false;
        }
    }

    /**
     * 删除指定时间点的军团公告。
     * Removes a legion announcement at the given timestamp.
     *
     * legion id
     * announcement timestamp
     */
    @Override
    public void removeAnnouncement(int legionId, Timestamp unixTime) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ANNOUNCEMENT_QUERY)) {

            stmt.setInt(1, legionId);
            stmt.setTimestamp(2, unixTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.eb7e7af87a03", legionId, e));
        }
    }

    /**
     * 持久化军团徽章：不存在则创建，需更新则更新。
     * Persists a legion emblem: inserts if missing, updates when required.
     *
     * legion id
     * legion emblem
     */
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
            log.error(I18n.get("log.5ee404f2d84f", legionid, e));
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
            log.error(I18n.get("log.0a3029c9bb09", legionId, e));
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
            log.error(I18n.get("log.746f2ebe921f", legionId, e));
        }
    }

    /**
     * 加载军团徽章；无记录时返回默认徽章。
     * Loads the legion emblem; returns a default emblem when none exists.
     *
     * legion id
     * legion emblem
     */
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
            log.error(I18n.get("log.256b0f63755e", legionId, e));
        }

        if (!found) {
            emblem.setEmblem(0, 0, 0, 0, LegionEmblemType.DEFAULT, null);
        }

        emblem.setPersistentState(PersistentState.UPDATED);
        return emblem;
    }

    /**
     * 加载军团仓库物品。
     * Loads the legion warehouse storage.
     *
     * legion
     * legion warehouse
     */
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
            log.error(I18n.get("log.d65e37a60b80", legionId, e));
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

    /**
     * 将军团历史记录加载到军团对象中。
     * Loads legion history entries into the legion instance.
     *
     * legion
     */
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
                        log.warn(I18n.get("log.cfe6b02e5699", legion.getLegionId(), resultSet.getString("history_type")));
                    }
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.c1a020fe3695", legion.getLegionId(), e));
        }
    }

    /**
     * 保存一条新的军团历史记录。
     * Saves a new legion history entry.
     *
     * legion id
     * history entry
     * whether succeeded
     */
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
            log.error(I18n.get("log.db8f371bd4f0", legionId, e));
            return false;
        }
    }

    /**
     * 更新军团描述、加入类型与最低加入等级。
     * Updates legion description, join type and minimum join level.
     *
     * legion
     */
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
            log.error(I18n.get("log.21551dfa9189", legion.getLegionId(), e));
        }
    }

    /**
     * 保存一条军团加入申请。
     * Stores a legion join request.
     *
     * join request
     */
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
            log.error(I18n.get("log.d118ce68ab6e", legionJoinRequest.getLegionId(), legionJoinRequest.getPlayerId(), e));
        }
    }

    /**
     * 加载军团全部加入申请。
     * Loads all join requests for the given legion.
     *
     * legion id
     *
     * @param legionId @return 加入申请列表 / join request list
     */
    @Override
    public List<LegionJoinRequest> loadLegionJoinRequests(final int legionId) {
        final List<LegionJoinRequest> requestList = new ArrayList<>();

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
            log.error(I18n.get("log.e21f88ccc6c6", legionId, e));
        }

        return requestList;
    }

    /**
     * 按军团 ID 与玩家 ID 删除加入申请。
     * Deletes a join request by legion id and player id.
     *
     * legion id
     * player id
     */
    @Override
    public void deleteLegionJoinRequest(int legionId, int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_RECRUIT_LIST_QUERY)) {

            stmt.setInt(1, legionId);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.c35e7fe03617", legionId, playerId, e));
        }
    }

    /**
     * 按申请对象删除加入申请。
     * Deletes a join request using the request object.
     *
     * @param ljr 加入申请 / join request
     */
    @Override
    public void deleteLegionJoinRequest(LegionJoinRequest ljr) {
        deleteLegionJoinRequest(ljr.getLegionId(), ljr.getPlayerId());
    }

    /**
     * 判断当前数据库是否受 MySQL 8 DAO 支持。
     * Returns whether the given database is supported by MySQL 8 DAOs.
     *
     * database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
