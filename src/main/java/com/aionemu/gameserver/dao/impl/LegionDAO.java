package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.model.team.legion.LegionEmblem;
import com.aionemu.gameserver.model.team.legion.LegionEmblemType;
import com.aionemu.gameserver.model.team.legion.LegionHistory;
import com.aionemu.gameserver.model.team.legion.LegionHistoryType;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequest;
import com.aionemu.gameserver.model.team.legion.LegionTerritory;
import com.aionemu.gameserver.model.team.legion.LegionWarehouse;

import lombok.extern.slf4j.Slf4j;

/**
 * 军团数据访问对象的 MySQL 8 实现。
 * MySQL 8 implementation of LegionDAO using JdbcClient.
 * @author Simple
 */
@Slf4j
@Repository
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

    private final JdbcClient jdbcClient;

    public LegionDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public LegionDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 检查军团名称是否已被占用。
     * Checks whether the given legion name is already used.
     * @param name 军团名称 / legion name
     * @return 是否已占用；查询失败时返回 true / whether used; true on query error
     */
    @Override
    public boolean isNameUsed(final String name) {
        try {
            return jdbcClient.sql(CHECK_NAME_USED_QUERY)
                .param(name)
                .query((rs, rowNum) -> rs.getInt("cnt") > 0)
                .optional()
                .orElse(false);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.84ada47a4666", name), e);
            return true;
        }
    }

    /**
     * 获取拥有领地（territory &gt; 0）的军团 ID 集合。
     * Returns legion ids that currently own a territory (territory &gt; 0).
     * @return 军团 ID 集合 / collection of legion ids
     */
    @Override
    public Collection<Integer> getLegionIdsWithTerritories() {
        Collection<Integer> legionIds = new ArrayList<>();

        try {
            legionIds.addAll(jdbcClient.sql(SELECT_LEGION_IDS_WITH_TERRITORIES)
                .query((rs, rowNum) -> rs.getInt("id"))
                .list());
        } catch (DataAccessException e) {
            log.error(I18n.get("log.3117e02da50f"), e);
        }

        return legionIds;
    }

    /**
     * 保存新建军团的基础记录（ID 与名称）。
     * Saves a newly created legion base row (id and name).
     * @param legion 军团 / legion
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean saveNewLegion(final Legion legion) {
        try {
            log.debug("Saving new legion: {} {}", legion.getLegionId(), legion.getLegionName());
            return jdbcClient.sql(INSERT_LEGION_QUERY)
                .param(legion.getLegionId())
                .param(legion.getLegionName())
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.5560e48eed9e", legion.getLegionId()), e);
            return false;
        }
    }

    /**
     * 持久化军团完整信息，并写入待处理的加入申请。
     * Persists full legion data and stores pending join requests.
     * @param legion 军团 / legion
     */
    @Override
    public void storeLegion(final Legion legion) {
        try {
            log.debug("Storing legion {} {}", legion.getLegionId(), legion.getLegionName());

            int territoryId = (legion.getTerritory() != null && legion.getTerritory().getId() > 0) ? legion.getTerritory().getId() : 0;

            jdbcClient.sql(UPDATE_LEGION_QUERY)
                .param(legion.getLegionName())
                .param(legion.getLegionLevel())
                .param(legion.getContributionPoints())
                .param(legion.getDeputyPermission())
                .param(legion.getCenturionPermission())
                .param(legion.getLegionaryPermission())
                .param(legion.getVolunteerPermission())
                .param(legion.getDisbandTime())
                .param(legion.getLegionDescription())
                .param(legion.getLegionJoinType())
                .param(legion.getMinLevel())
                .param(territoryId)
                .param(legion.getLegionId())
                .update();

            // 存储加入请求 / Store join requests
            if (!legion.getJoinRequestMap().isEmpty()) {
                for (LegionJoinRequest ljr : legion.getJoinRequestMap().values()) {
                    storeLegionJoinRequest(ljr);
                }
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.9f74efb13ee5", legion.getLegionId()), e);
        }
    }

    /**
     * 按名称加载军团。
     * Loads a legion by name.
     * @param legionName 军团名 / legion name
     * @return 军团；不存在时返回 null / legion, or null if missing
     */
    @Override
    public Legion loadLegion(final String legionName) {
        Legion legion = null;

        try {
            legion = jdbcClient.sql(SELECT_LEGION_QUERY2)
                .param(legionName)
                .query((resultSet, rowNum) -> createLegionFromResultSet(resultSet))
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ef450357e2bb", legionName), e);
        }

        log.debug("Loaded legion: {}", legion != null ? legion.getLegionId() : "null");
        return legion;
    }

    /**
     * 按 ID 加载军团，并附加加入申请列表。
     * Loads a legion by id and attaches join requests.
     * @param legionId 军团 ID / legion id
     * @return 军团；不存在时返回 null / legion, or null if missing
     */
    @Override
    public Legion loadLegion(final int legionId) {
        Legion legion = null;

        try {
            legion = jdbcClient.sql(SELECT_LEGION_QUERY1)
                .param(legionId)
                .query((resultSet, rowNum) -> {
                    Legion l = createLegionFromResultSet(resultSet);

                    // 加载加入请求 / Load join requests
                    for (LegionJoinRequest ljr : loadLegionJoinRequests(l.getLegionId())) {
                        l.addJoinRequest(ljr);
                    }
                    return l;
                })
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.bd9d366b0d7a", legionId), e);
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
     * @param legionId 军团 ID / legion id
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
            log.error(I18n.get("log.9be4d3aa4400", legionId), e);
        }
    }

    /**
     * 获取已占用的军团 ID 列表，供 ID 工厂使用。
     * Returns used legion ids for ID factory allocation.
     * @return 已占用 ID 数组；出错时返回空数组 / used id array, or empty on error.
     */
    @Override
    public int[] getUsedIDs() {
        try {
            List<Integer> ids = jdbcClient.sql(SELECT_USED_IDS_QUERY)
                .query((rs, rowNum) -> rs.getInt("id"))
                .list();

            int[] result = new int[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                result[i] = ids.get(i);
            }
            return result;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.82d77c44202e"), e);
            return new int[0];
        }
    }

    /**
     * 加载军团公告列表（最多 7 条，按时间升序）。
     * Loads legion announcements (up to 7, ordered by date ascending).
     * @param legionId 军团 ID / legion id
     * @return 时间戳到公告内容的有序映射 / ordered map of timestamp to message
     */
    @Override
    public TreeMap<Timestamp, String> loadAnnouncementList(final int legionId) {
        final TreeMap<Timestamp, String> announcementList = new TreeMap<>();

        try {
            jdbcClient.sql(SELECT_ANNOUNCEMENTLIST_QUERY)
                .param(legionId)
                .query((resultSet, rowNum) -> {
                    String message = resultSet.getString("announcement");
                    Timestamp date = resultSet.getTimestamp("date");
                    announcementList.put(date, message);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.c4ec74ecbd88", legionId), e);
        }

        log.debug("Loaded announcement list for legion: {}", legionId);
        return announcementList;
    }

    /**
     * 保存一条新的军团公告。
     * Saves a new legion announcement.
     * @param legionId 军团 ID / legion id
     * @param currentTime 公告时间戳 / announcement timestamp
     * @param message 公告消息 / announcement message
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean saveNewAnnouncement(final int legionId, final Timestamp currentTime, final String message) {
        try {
            log.debug("Saving new announcement for legion: {}", legionId);
            return jdbcClient.sql(INSERT_ANNOUNCEMENT_QUERY)
                .param(legionId)
                .param(message)
                .param(currentTime)
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.d7d4b79baa01", legionId), e);
            return false;
        }
    }

    /**
     * 删除指定时间点的军团公告。
     * Removes a legion announcement at the given timestamp.
     * @param legionId 军团 ID / legion id
     * @param unixTime 公告时间戳 / announcement timestamp
     */
    @Override
    public void removeAnnouncement(int legionId, Timestamp unixTime) {
        try {
            jdbcClient.sql(DELETE_ANNOUNCEMENT_QUERY)
                .param(legionId)
                .param(unixTime)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.eb7e7af87a03", legionId), e);
        }
    }

    /**
     * 持久化军团徽章：不存在则创建，需更新则更新。
     * Persists a legion emblem: inserts if missing, updates when required.
     * @param legionId 军团 ID / legion id
     * @param legionEmblem 军团徽章 / legion emblem
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
        try {
            return jdbcClient.sql(SELECT_EMBLEM_QUERY)
                .param(legionid)
                .query((rs, rowNum) -> true)
                .optional()
                .orElse(false);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.5ee404f2d84f", legionid), e);
            return false;
        }
    }

    private void createLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
        try {
            jdbcClient.sql(INSERT_EMBLEM_QUERY)
                .param(legionId)
                .param(legionEmblem.getEmblemId())
                .param(legionEmblem.getColor_r())
                .param(legionEmblem.getColor_g())
                .param(legionEmblem.getColor_b())
                .param(legionEmblem.getEmblemType().toString())
                .param(legionEmblem.getCustomEmblemData())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.0a3029c9bb09", legionId), e);
        }
    }

    private void updateLegionEmblem(final int legionId, final LegionEmblem legionEmblem) {
        try {
            jdbcClient.sql(UPDATE_EMBLEM_QUERY)
                .param(legionEmblem.getEmblemId())
                .param(legionEmblem.getColor_r())
                .param(legionEmblem.getColor_g())
                .param(legionEmblem.getColor_b())
                .param(legionEmblem.getEmblemType().toString())
                .param(legionEmblem.getCustomEmblemData())
                .param(legionId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.746f2ebe921f", legionId), e);
        }
    }

    /**
     * 加载军团徽章；无记录时返回默认徽章。
     * Loads the legion emblem; returns a default emblem when none exists.
     * @param legionId 军团 ID / legion id
     * @return 军团徽章 / legion emblem
     */
    @Override
    public LegionEmblem loadLegionEmblem(final int legionId) {
        LegionEmblem emblem = new LegionEmblem();
        boolean found = false;

        try {
            found = jdbcClient.sql(SELECT_EMBLEM_QUERY)
                .param(legionId)
                .query((resultSet, rowNum) -> {
                    emblem.setEmblem(
                        resultSet.getInt("emblem_id"),
                        resultSet.getInt("color_r"),
                        resultSet.getInt("color_g"),
                        resultSet.getInt("color_b"),
                        LegionEmblemType.valueOf(resultSet.getString("emblem_type")),
                        resultSet.getBytes("emblem_data")
                    );
                    return true;
                })
                .optional()
                .orElse(false);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.256b0f63755e", legionId), e);
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
     * @param legion 军团 / legion
     * @return 军团仓库 / legion warehouse
     */
    @Override
    public LegionWarehouse loadLegionStorage(Legion legion) {
        final LegionWarehouse inventory = new LegionWarehouse(legion);
        final int legionId = legion.getLegionId();
        final int storage = StorageType.LEGION_WAREHOUSE.getId();
        final int equipped = 0;

        try {
            jdbcClient.sql(SELECT_STORAGE_QUERY)
                .param(legionId)
                .param(storage)
                .param(equipped)
                .query((rset, rowNum) -> {
                    Item item = constructItem(rset, storage);
                    item.setPersistentState(PersistentState.UPDATED);
                    inventory.onLoadHandler(item);
                    return null;
                })
                .list();
        } catch (Exception e) {
            log.error(I18n.get("log.d65e37a60b80", legionId), e);
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
     * @param legion 军团 / legion
     */
    @Override
    public void loadLegionHistory(final Legion legion) {
        final Collection<LegionHistory> history = legion.getLegionHistory();

        try {
            jdbcClient.sql(SELECT_HISTORY_QUERY)
                .param(legion.getLegionId())
                .query((resultSet, rowNum) -> {
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
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.c1a020fe3695", legion.getLegionId()), e);
        }
    }

    /**
     * 保存一条新的军团历史记录。
     * Saves a new legion history entry.
     * @param legionId 军团 ID / legion id
     * @param legionHistory history entry
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean saveNewLegionHistory(final int legionId, final LegionHistory legionHistory) {
        try {
            return jdbcClient.sql(INSERT_HISTORY_QUERY)
                .param(legionId)
                .param(legionHistory.getTime())
                .param(legionHistory.getLegionHistoryType().toString())
                .param(legionHistory.getName())
                .param(legionHistory.getTabId())
                .param(legionHistory.getDescription())
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.db8f371bd4f0", legionId), e);
            return false;
        }
    }

    /**
     * 更新军团描述、加入类型与最低加入等级。
     * Updates legion description, join type and minimum join level.
     * @param legion 军团 / legion
     */
    @Override
    public void updateLegionDescription(final Legion legion) {
        try {
            jdbcClient.sql(UPDATE_LEGION_DESCRIPTION_QUERY)
                .param(legion.getLegionDescription())
                .param(legion.getLegionJoinType())
                .param(legion.getMinLevel())
                .param(legion.getLegionId())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.21551dfa9189", legion.getLegionId()), e);
        }
    }

    /**
     * 保存一条军团加入申请。
     * Stores a legion join request.
     * @param legionJoinRequest 加入请求 / join request
     */
    @Override
    public void storeLegionJoinRequest(final LegionJoinRequest legionJoinRequest) {
        try {
            jdbcClient.sql(INSERT_RECRUIT_LIST_QUERY)
                .param(legionJoinRequest.getLegionId())
                .param(legionJoinRequest.getPlayerId())
                .param(legionJoinRequest.getPlayerName())
                .param(legionJoinRequest.getPlayerClass())
                .param(legionJoinRequest.getRace())
                .param(legionJoinRequest.getLevel())
                .param(legionJoinRequest.getGenderId())
                .param(legionJoinRequest.getMsg())
                .param(legionJoinRequest.getDate())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.d118ce68ab6e", legionJoinRequest.getLegionId(),
                    legionJoinRequest.getPlayerId()), e);
        }
    }

    /**
     * 加载军团全部加入申请。
     * Loads all join requests for the given legion.
     * @param legionId 军团 ID / legion id
     * @return 加入申请列表 / join request list
     */
    @Override
    public List<LegionJoinRequest> loadLegionJoinRequests(final int legionId) {
        final List<LegionJoinRequest> requestList = new ArrayList<>();

        try {
            requestList.addAll(jdbcClient.sql(SELECT_RECRUIT_LIST_QUERY)
                .param(legionId)
                .query((resultSet, rowNum) -> {
                    LegionJoinRequest ljr = new LegionJoinRequest();
                    ljr.setLegionId(resultSet.getInt("legionId"));
                    ljr.setPlayerId(resultSet.getInt("playerId"));
                    ljr.setPlayerName(resultSet.getString("playerName"));
                    ljr.setPlayerClass(resultSet.getInt("playerClassId"));
                    ljr.setRace(resultSet.getInt("playerRaceId"));
                    ljr.setLevel(resultSet.getInt("playerLevel"));
                    ljr.setGenderId(resultSet.getInt("playerGenderId"));
                    ljr.setDate(resultSet.getTimestamp("date"));
                    return ljr;
                })
                .list());
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e21f88ccc6c6", legionId), e);
        }

        return requestList;
    }

    /**
     * 按军团 ID 与玩家 ID 删除加入申请。
     * Deletes a join request by legion id and player id.
     * @param legionId 军团 ID / legion id
     * @param playerId 玩家 ID / player id
     */
    @Override
    public void deleteLegionJoinRequest(int legionId, int playerId) {
        try {
            jdbcClient.sql(DELETE_RECRUIT_LIST_QUERY)
                .param(legionId)
                .param(playerId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.c35e7fe03617", legionId, playerId), e);
        }
    }

    /**
     * 按申请对象删除加入申请。
     * Deletes a join request using the request object.
     * @param ljr 加入申请 / join request
     */
    @Override
    public void deleteLegionJoinRequest(LegionJoinRequest ljr) {
        deleteLegionJoinRequest(ljr.getLegionId(), ljr.getPlayerId());
    }

    /**
     * 判断当前数据库是否受 MySQL 8 DAO 支持。
     * Returns whether the given database is supported by MySQL 8 DAOs.
     * @param databaseName 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
