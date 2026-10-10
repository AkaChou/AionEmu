package com.aionemu.gameserver.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.configs.main.CacheConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerInitialData;
import com.aionemu.gameserver.dataholders.PlayerInitialData.LocationData;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.account.PlayerAccountData;
import com.aionemu.gameserver.model.gameobjects.player.Mailbox;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerUpgradeArcade;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;
import com.aionemu.gameserver.model.templates.portal.InstanceExit;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家数据访问对象的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerDAO using JdbcClient.
 */
@Slf4j
public class PlayerDAO extends com.aionemu.gameserver.dao.PlayerDAO {

    /** 按对象 ID 缓存的玩家公共数据 / Player common data cache by object ID */
    private final Map<Integer, PlayerCommonData> playerCommonData = new ConcurrentHashMap<>();
    /** 按角色名（小写）缓存的玩家公共数据 / Player common data cache by name (lowercase) */
    private final Map<String, PlayerCommonData> playerCommonDataByName = new ConcurrentHashMap<>();

    // 查询 / Queries
    /** 检查角色名是否占用 / Check if character name is used */
    private static final String CHECK_NAME_USED_QUERY = "SELECT COUNT(id) as cnt FROM players WHERE name = ?";
    /** 按 ID 列表批量查询角色名 / Select player names by ID list */
    private static final String SELECT_PLAYER_NAMES_QUERY = "SELECT id, `name` FROM players WHERE id IN (%s)";
    /** 更新玩家完整数据 / Update full player row */
    private static final String UPDATE_PLAYER_QUERY = "UPDATE players SET " + "name = ?, exp = ?, recoverexp = ?, x = ?, y = ?, z = ?, heading = ?, " + "world_id = ?, gender = ?, race = ?, player_class = ?, last_online = ?, " + "quest_expands = ?, npc_expands = ?, advenced_stigma_slot_size = ?, " + "warehouse_size = ?, note = ?, title_id = ?, bonus_title_id = ?, " + "dp = ?, soul_sickness = ?, mailbox_letters = ?, reposte_energy = ?, " + "mentor_flag_time = ?, world_owner = ?, stamps = ?, rewarded_pass = ?, " + "last_stamp = ?, passport_time = ?, is_archdaeva = ?, creativity_point = ?, " + "aura_of_growth = ?, join_legion_id = ?, join_state = ?, berdin_star = ?, " + "abyss_favor = ?, luna_consume = ?, muni_keys = ?, luna_consume_count = ?, " + "wardrobe_slot = ?, frenzy_points = ?, frenzy_count = ?, toc_floor = ?, " + "stone_cp = ?, golden_dice = ?, sweep_reset = ?, minion_skill_points = ?, " + "minion_function_time = ? WHERE id = ?";

    /** 插入新角色 / Insert a new player */
    private static final String INSERT_PLAYER_QUERY = "INSERT INTO players " + "(id, `name`, account_id, account_name, x, y, z, heading, " + "world_id, gender, race, player_class, quest_expands, npc_expands, " + "warehouse_size, bonus_title_id, is_archdaeva,wardrobe_slot, " + "online, stamps, rewarded_pass) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 2, 0, 0, 1)";

    /** 按角色名查询对象 ID / Select player ID by name */
    private static final String SELECT_PLAYER_ID_BY_NAME_QUERY = "SELECT id FROM players WHERE name = ?";
    /** 按对象 ID 查询完整玩家行 / Select full player row by ID */
    private static final String SELECT_PLAYER_COMMON_DATA_QUERY = "SELECT * FROM players WHERE id = ?";
    /** 删除角色 / Delete player by ID */
    private static final String DELETE_PLAYER_QUERY = "DELETE FROM players WHERE id = ?";
    /** 查询账号下所有角色 ID / Select player IDs on an account */
    private static final String SELECT_PLAYER_OIDS_ON_ACCOUNT_QUERY = "SELECT id FROM players WHERE account_id = ?";
    /** 查询创建/删除时间 / Select creation and deletion timestamps */
    private static final String SELECT_CREATION_DELETION_TIME_QUERY = "SELECT creation_date, deletion_date FROM players WHERE id = ?";
    /** 更新删除时间 / Update deletion date */
    private static final String UPDATE_DELETION_TIME_QUERY = "UPDATE players SET deletion_date = ? WHERE id = ?";
    /** 更新创建时间 / Update creation date */
    private static final String UPDATE_CREATION_TIME_QUERY = "UPDATE players SET creation_date = ? WHERE id = ?";
    /** 更新最后在线时间 / Update last online time */
    private static final String UPDATE_LAST_ONLINE_QUERY = "UPDATE players SET last_online = ? WHERE id = ?";
    /** 查询所有已用角色 ID / Select all used player IDs */
    private static final String SELECT_USED_IDS_QUERY = "SELECT id FROM players";
    /** 更新单个玩家在线状态 / Update single player online flag */
    private static final String UPDATE_ONLINE_STATUS_QUERY = "UPDATE players SET online = ? WHERE id = ?";
    /** 批量更新全部玩家在线状态 / Update all players online flag */
    private static final String UPDATE_ALL_ONLINE_QUERY = "UPDATE players SET online = ?";
    /** 按对象 ID 查询角色名 / Select player name by object ID */
    private static final String SELECT_PLAYER_NAME_BY_OBJ_ID_QUERY = "SELECT name FROM players WHERE id = ?";
    /** 按对象 ID 查询月神消费点数 / Select luna consume points by object ID */
    private static final String SELECT_LUNA_CONSUME_BY_OBJ_ID_QUERY = "SELECT luna_consume FROM players WHERE id = ?";
    /** 按角色名查询对象 ID / Select player ID by name */
    private static final String SELECT_PLAYER_ID_BY_NAME = "SELECT id FROM players WHERE name = ?";
    /** 按角色名查询账号 ID / Select account ID by player name */
    private static final String SELECT_ACCOUNT_ID_BY_NAME_QUERY = "SELECT `account_id` FROM `players` WHERE `name` = ?";
    /** 更新角色名 / Update player name */
    private static final String UPDATE_PLAYER_NAME_QUERY = "UPDATE players SET name = ? WHERE id = ?";
    /** 统计账号下有效角色数 / Count active characters on account */
    private static final String SELECT_CHARACTER_COUNT_QUERY = "SELECT COUNT(*) AS cnt FROM `players` WHERE `account_id` = ? " + "AND (deletion_date IS NULL OR deletion_date > CURRENT_TIMESTAMP)";
    /** 按种族统计达标角色账号数 / Count distinct accounts by race above exp threshold */
    private static final String SELECT_CHARACTER_COUNT_RACE_QUERY = "SELECT COUNT(DISTINCT(`account_name`)) AS `count` FROM `players` " + "WHERE `race` = ? AND `exp` >= ?";
    /** 统计在线玩家数 / Count online players */
    private static final String SELECT_ONLINE_PLAYER_COUNT_QUERY = "SELECT COUNT(*) AS `count` FROM `players` WHERE `online` = ?";
    /** 查询因不活跃待删除的角色 / Select inactive players to delete */
    private static final String SELECT_PLAYERS_TO_DELETE_QUERY = "SELECT id FROM players WHERE UNIX_TIMESTAMP(CURDATE()) - " + "UNIX_TIMESTAMP(last_online) > ? * 24 * 60 * 60";
    /** 更新上次转服时间 / Update last transfer time */
    private static final String UPDATE_LAST_TRANSFER_TIME_QUERY = "UPDATE players SET last_transfer_time = ? WHERE id = ?";
    /** 查询角色创建时间 / Select character creation date */
    private static final String SELECT_CREATION_DATE_QUERY = "SELECT `creation_date` FROM `players` WHERE `id` = ?";
    /** 更新军团申请状态 / Update legion join request state */
    private static final String UPDATE_JOIN_STATE_QUERY = "UPDATE players SET join_state = ? WHERE id = ?";
    /** 清除军团申请 / Clear legion join request */
    private static final String CLEAR_JOIN_REQUEST_QUERY = "UPDATE players SET join_legion_id = ?, join_state = ? WHERE id = ?";
    /** 查询军团申请状态 / Select legion join request state */
    private static final String SELECT_JOIN_STATE_QUERY = "SELECT join_state FROM players WHERE id = ?";

    /** 更新玩家任务奖励列（含 durable 奖励的 exp/aura_of_growth）/ Update player quest-reward columns */
    private static final String UPDATE_QUEST_CURRENCIES_QUERY = "UPDATE players SET exp = ?, dp = ?, creativity_point = ?, abyss_favor = ?, aura_of_growth = ? WHERE id = ?";
    /** 在任务事务中持久化高阶守护者晋升 / Persist ArchDaeva promotion in the quest transaction */
    private static final String PROMOTE_ARCHDAEVA_QUERY = "UPDATE players SET exp = GREATEST(exp, ?), is_archdaeva = TRUE WHERE id = ?";

    private final JdbcClient jdbcClient;

    public PlayerDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public PlayerDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 检查角色名是否已使用。
     * Checks whether a character name is already used.
     * @param name 角色名 / character name
     * @return 若已使用为 true；查询异常时也返回 true / true if used, or on query error
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
            log.error(I18n.get("log.b38241da2c4e", name), e);
            return true;
        }
    }

    /**
     * 按对象 ID 集合批量查询角色名。
     * Loads player names for the given object IDs.
     * @param playerObjectIds 玩家对象 ID 集合 / collection of player object IDs
     * @return ID 到角色名的映射；入参为空时返回空 Map / map of id to name, empty if input blank
     */
    @Override
    public Map<Integer, String> getPlayerNames(Collection<Integer> playerObjectIds) {
        if (GenericValidator.isBlankOrNull(playerObjectIds)) {
            return Collections.emptyMap();
        }

        Map<Integer, String> result = Maps.newHashMap();
        String sql = String.format(SELECT_PLAYER_NAMES_QUERY, StringUtils.join(playerObjectIds, ", "));

        try {
            jdbcClient.sql(sql)
                .query((rs, rowNum) -> {
                    result.put(rs.getInt("id"), rs.getString("name"));
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.25f109fb2408"), e);
        }

        return result;
    }

    /**
     * 将在线玩家的完整数据持久化到数据库，并按配置更新缓存。
     * Persists a player's full data to the database and refreshes caches when enabled.
     * @param player 待保存的玩家 / player to store
     */
    @Override
    public void storePlayer(final Player player) {
        try {
            log.debug("Storing player {} {}", player.getObjectId(), player.getName());
            PlayerCommonData pcd = player.getCommonData();

            Mailbox mailBox = player.getMailbox();
            int mails = mailBox != null ? mailBox.size() : pcd.getMailboxLetters();

            PlayerUpgradeArcade pua = player.getUpgradeArcade();
            int frenzyPoints = pua != null ? pua.getFrenzyPoints() : 0;
            int frenzyCount = pua != null ? pua.getFrenzyCount() : 0;

            jdbcClient.sql(UPDATE_PLAYER_QUERY)
                .param(player.getName())
                .param(pcd.getExp())
                .param(pcd.getExpRecoverable())
                .param(player.getX())
                .param(player.getY())
                .param(player.getZ())
                .param(player.getHeading())
                .param(player.getWorldId())
                .param(player.getGender().toString())
                .param(player.getRace().toString())
                .param(pcd.getPlayerClass().toString())
                .param(pcd.getLastOnline())
                .param(player.getQuestExpands())
                .param(player.getNpcExpands())
                .param(pcd.getAdvancedStigmaSlotSize())
                .param(player.getWarehouseSize())
                .param(pcd.getNote())
                .param(pcd.getTitleId())
                .param(pcd.getBonusTitleId())
                .param(pcd.getDp())
                .param(pcd.getDeathCount())
                .param(mails)
                .param(pcd.getCurrentReposteEnergy())
                .param(pcd.getMentorFlagTime())
                .param(player.getPosition().getWorldMapInstance().getOwnerId())
                .param(pcd.getPassportStamps())
                .param(pcd.getPassportReward())
                .param(pcd.getLastStamp())
                .param(pcd.getPassportTime())
                .param(pcd.isArchDaeva())
                .param(pcd.getCreativityPoint())
                .param(pcd.getAuraOfGrowth())
                .param(pcd.getJoinRequestLegionId())
                .param(pcd.getJoinRequestState().toString())
                .param(pcd.getBerdinStar())
                .param(pcd.getAbyssFavor())
                .param(pcd.getLunaConsumePoint())
                .param(pcd.getMuniKeys())
                .param(pcd.getLunaConsumeCount())
                .param(pcd.getWardrobeSlot())
                .param(frenzyPoints)
                .param(frenzyCount)
                .param(pcd.getFloor())
                .param(pcd.getStoneCreativityPoint())
                .param(pcd.getGoldenDice())
                .param(pcd.getResetBoard())
                .param(pcd.getMinionSkillPoints())
                .param(pcd.getMinionFunctionTime())
                .param(player.getObjectId())
                .update();

        } catch (DataAccessException e) {
            log.error(I18n.get("log.143d7f893e9c", player.getObjectId(), player.getName()), e);
        }

        if (CacheConfig.CACHE_COMMONDATA) {
            playerCommonData.put(player.getObjectId(), player.getCommonData());
            playerCommonDataByName.put(player.getName().toLowerCase(), player.getCommonData());
        }
    }

    @Override
    public void storeInTransaction(Connection con, int playerId, PlayerCommonData pcd) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUEST_CURRENCIES_QUERY)) {
            stmt.setLong(1, pcd.getExp());
            stmt.setInt(2, pcd.getDp());
            stmt.setInt(3, pcd.getCreativityPoint());
            stmt.setLong(4, pcd.getAbyssFavor());
            stmt.setLong(5, pcd.getAuraOfGrowth());
            stmt.setInt(6, playerId);
            if (stmt.executeUpdate() != 1) {
                throw new SQLException("No player row changed for player " + playerId);
            }
        }
    }

    @Override
    public void promoteArchDaevaInTransaction(Connection connection, int playerId, long minimumExp) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(PROMOTE_ARCHDAEVA_QUERY)) {
            statement.setLong(1, minimumExp);
            statement.setInt(2, playerId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No player row changed for player " + playerId);
            }
        }
    }

    /**
     * 插入新创建的角色记录，并按配置写入缓存。
     * Inserts a newly created character and updates caches when enabled.
     * @param pcd 新角色公共数据 / new player common data
     * @param accountId 账号 ID / account id
     * @param accountName 账号名 / account name
     * @return 成功为 true，失败为 false / true on success, false on failure
     */
    @Override
    public boolean saveNewPlayer(final PlayerCommonData pcd, final int accountId, final String accountName) {
        try {
            log.debug("Saving new player: {} {}", pcd.getPlayerObjId(), pcd.getName());

            jdbcClient.sql(INSERT_PLAYER_QUERY)
                .param(pcd.getPlayerObjId())
                .param(pcd.getName())
                .param(accountId)
                .param(accountName)
                .param(pcd.getPosition().getX())
                .param(pcd.getPosition().getY())
                .param(pcd.getPosition().getZ())
                .param(pcd.getPosition().getHeading())
                .param(pcd.getPosition().getMapId())
                .param(pcd.getGender().toString())
                .param(pcd.getRace().toString())
                .param(pcd.getPlayerClass().toString())
                .param(pcd.getQuestExpands())
                .param(pcd.getNpcExpands())
                .param(pcd.getWarehouseSize())
                .param(pcd.getBonusTitleId())
                .param(pcd.isArchDaeva())
                .update();

            if (CacheConfig.CACHE_COMMONDATA) {
                playerCommonData.put(pcd.getPlayerObjId(), pcd);
                playerCommonDataByName.put(pcd.getName().toLowerCase(), pcd);
            }

            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.fc97fe191787", pcd.getPlayerObjId(), pcd.getName()), e);
            return false;
        }
    }

    /**
     * 按角色名加载玩家公共数据（优先世界在线与缓存）。
     * Loads player common data by name (world online and cache first).
     * @param name 角色名 / character name
     * @return 公共数据，未找到时为 null / common data, or null if not found
     */
    @Override
    public PlayerCommonData loadPlayerCommonDataByName(final String name) {
        Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(name);
        if (player != null) {
            return player.getCommonData();
        }

        PlayerCommonData pcd = playerCommonDataByName.get(name.toLowerCase());
        if (pcd != null) {
            return pcd;
        }

        int playerObjId = 0;
        try {
            playerObjId = jdbcClient.sql(SELECT_PLAYER_ID_BY_NAME_QUERY)
                .param(name)
                .query((rset, rowNum) -> rset.getInt("id"))
                .optional()
                .orElse(0);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.cbf76d90931d", name), e);
        }

        return playerObjId > 0 ? loadPlayerCommonData(playerObjId) : null;
    }

    /**
     * 按对象 ID 加载玩家公共数据（优先缓存，必要时回退出生点）。
     * Loads player common data by object ID (cache first; may fall back to spawn).
     * @param playerObjId 玩家对象 ID / player object id
     * @return 公共数据，未找到时为 null / common data, or null if not found
     */
    @Override
    public PlayerCommonData loadPlayerCommonData(final int playerObjId) {
        PlayerCommonData cached = playerCommonData.get(playerObjId);
        if (cached != null) {
            log.debug("PlayerCommonData for id: {} obtained from cache", playerObjId);
            return cached;
        }

        final PlayerCommonData cd = new PlayerCommonData(playerObjId);

        try {
            log.debug("Loading player from db: {}", playerObjId);

            boolean found = jdbcClient.sql(SELECT_PLAYER_COMMON_DATA_QUERY)
                .param(playerObjId)
                .query((resultSet, rowNum) -> {
                    cd.setName(resultSet.getString("name"));

                    try {
                        cd.setPlayerClass(PlayerClass.valueOf(resultSet.getString("player_class")));
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.40c87a64d2f0", playerObjId, resultSet.getString("player_class")));
                        cd.setPlayerClass(PlayerClass.WARRIOR);
                    }

                    cd.setExp(resultSet.getLong("exp"), false);
                    cd.setRecoverableExp(resultSet.getLong("recoverexp"));

                    try {
                        cd.setRace(Race.valueOf(resultSet.getString("race")));
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.1106f1274fe9", playerObjId, resultSet.getString("race")));
                        cd.setRace(Race.ELYOS);
                    }

                    try {
                        cd.setGender(Gender.valueOf(resultSet.getString("gender")));
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.2eff1e6d34b2", playerObjId, resultSet.getString("gender")));
                        cd.setGender(Gender.MALE);
                    }

                    cd.setLastOnline(resultSet.getTimestamp("last_online"));
                    cd.setNote(resultSet.getString("note"));
                    cd.setQuestExpands(resultSet.getInt("quest_expands"));
                    cd.setNpcExpands(resultSet.getInt("npc_expands"));
                    cd.setAdvancedStigmaSlotSize(resultSet.getInt("advenced_stigma_slot_size"));
                    cd.setTitleId(resultSet.getInt("title_id"));
                    cd.setBonusTitleId(resultSet.getInt("bonus_title_id"));
                    cd.setWarehouseSize(resultSet.getInt("warehouse_size"));
                    cd.setOnline(resultSet.getBoolean("online"));
                    cd.setMailboxLetters(resultSet.getInt("mailbox_letters"));
                    cd.setDp(resultSet.getInt("dp"));
                    cd.setDeathCount(resultSet.getInt("soul_sickness"));
                    cd.setCurrentReposteEnergy(resultSet.getLong("reposte_energy"));

                    float x = resultSet.getFloat("x");
                    float y = resultSet.getFloat("y");
                    float z = resultSet.getFloat("z");
                    byte heading = resultSet.getByte("heading");
                    int worldId = resultSet.getInt("world_id");

                    PlayerInitialData playerInitialData = DataManager.PLAYER_INITIAL_DATA;
                    WorldMapInstance mainInstance = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).getMainWorldMapInstance();
                    MapRegion mr = mainInstance == null ? null : mainInstance.getRegion(x, y, z);

                    if (mr == null) {
                        InstanceExit exit = DataManager.INSTANCE_EXIT_DATA == null ? null : DataManager.INSTANCE_EXIT_DATA.getInstanceExit(worldId, cd.getRace());
                        if (exit != null) {
                            x = exit.getX();
                            y = exit.getY();
                            z = exit.getZ();
                            heading = exit.getH();
                            worldId = exit.getExitWorld();
                        } else if (playerInitialData != null) {
                            LocationData ld = playerInitialData.getSpawnLocation(cd.getRace());
                            if (ld != null) {
                                x = ld.getX();
                                y = ld.getY();
                                z = ld.getZ();
                                heading = ld.getHeading();
                                worldId = ld.getMapId();
                            }
                        }
                    }

                    WorldPosition position = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().createPosition(worldId, x, y, z, heading, 0);
                    cd.setPosition(position);

                    cd.setWorldOwnerId(resultSet.getInt("world_owner"));
                    cd.setMentorFlagTime(resultSet.getInt("mentor_flag_time"));
                    cd.setLastTransferTime(resultSet.getLong("last_transfer_time"));
                    cd.setPassportStamps(resultSet.getInt("stamps"));
                    cd.setPassportReward(resultSet.getInt("rewarded_pass"));
                    cd.setLastStamp(resultSet.getTimestamp("last_stamp"));
                    cd.setPassportTime(resultSet.getInt("passport_time"));
                    cd.setArchDaeva(resultSet.getBoolean("is_archdaeva"));
                    cd.setCreativityPoint(resultSet.getInt("creativity_point"));
                    cd.addAuraOfGrowth(resultSet.getLong("aura_of_growth"));
                    cd.setJoinRequestLegionId(resultSet.getInt("join_legion_id"));

                    try {
                        cd.setJoinRequestState(LegionJoinRequestState.valueOf(resultSet.getString("join_state")));
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.2ab35f9897c1", playerObjId, resultSet.getString("join_state")));
                        cd.setJoinRequestState(LegionJoinRequestState.NONE);
                    }

                    cd.addBerdinStar(resultSet.getLong("berdin_star"));
                    cd.addAbyssFavor(resultSet.getLong("abyss_favor"));
                    cd.setLunaConsumePoint(resultSet.getInt("luna_consume"));
                    cd.setMuniKeys(resultSet.getInt("muni_keys"));
                    cd.setLunaConsumeCount(resultSet.getInt("luna_consume_count"));
                    cd.setWardrobeSlot(resultSet.getInt("wardrobe_slot"));

                    PlayerUpgradeArcade pua = new PlayerUpgradeArcade();
                    pua.setFrenzyPoints(resultSet.getInt("frenzy_points"));
                    pua.setFrenzyCount(resultSet.getInt("frenzy_count"));

                    cd.setFloor(resultSet.getInt("toc_floor"));
                    cd.setStoneCreativityPoint(resultSet.getInt("stone_cp"));
                    cd.setGoldenDice(resultSet.getInt("golden_dice"));
                    cd.setResetBoard(resultSet.getInt("sweep_reset"));
                    cd.setMinionSkillPoints(resultSet.getInt("minion_skill_points"));
                    cd.setMinionFunctionTime(resultSet.getTimestamp("minion_function_time"));

                    if (CacheConfig.CACHE_COMMONDATA) {
                        playerCommonData.put(playerObjId, cd);
                        playerCommonDataByName.put(cd.getName().toLowerCase(), cd);
                    }

                    return true;
                })
                .optional()
                .orElse(false);

            if (found) {
                return cd;
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ccf8db4780c8", playerObjId), e);
        }

        return null;
    }

    /**
     * 按对象 ID 删除角色，并同步清理缓存。
     * Deletes a player by object ID and clears related caches.
     * @param playerId 玩家对象 ID / player object id
     */
    @Override
    public void deletePlayer(int playerId) {
        try {
            jdbcClient.sql(DELETE_PLAYER_QUERY)
                .param(playerId)
                .update();

            if (CacheConfig.CACHE_COMMONDATA) {
                PlayerCommonData pcd = playerCommonData.remove(playerId);
                if (pcd != null) {
                    playerCommonDataByName.remove(pcd.getName().toLowerCase());
                }
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.bb5fb346ba48", playerId), e);
        }
    }

    /**
     * 查询指定账号下全部角色对象 ID。
     * Returns all player object IDs belonging to an account.
     * @param accountId 账号 ID / account id
     * @return 角色 ID 列表；查询异常时为 null / list of player ids, or null on error
     */
    @Override
    public List<Integer> getPlayerOidsOnAccount(final int accountId) {
        try {
            return jdbcClient.sql(SELECT_PLAYER_OIDS_ON_ACCOUNT_QUERY)
                .param(accountId)
                .query((resultSet, rowNum) -> resultSet.getInt("id"))
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e310f2d2c9cd", accountId), e);
            return null;
        }
    }

    /**
     * 从数据库读取并填充账号角色的创建/删除时间。
     * Loads creation and deletion timestamps into account character data.
     * @param acData 账号角色数据 / player account data
     */
    @Override
    public void setCreationDeletionTime(final PlayerAccountData acData) {
        try {
            jdbcClient.sql(SELECT_CREATION_DELETION_TIME_QUERY)
                .param(acData.getPlayerCommonData().getPlayerObjId())
                .query((rset, rowNum) -> {
                    acData.setDeletionDate(rset.getTimestamp("deletion_date"));
                    acData.setCreationDate(rset.getTimestamp("creation_date"));
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8949342fe4a2", acData.getPlayerCommonData().getPlayerObjId()), e);
        }
    }

    /**
     * 更新角色的删除时间。
     * Updates a character's deletion timestamp.
     * @param objectId 玩家对象 ID / player object id
     * @param deletionDate 删除时间戳 / deletion timestamp
     */
    @Override
    public void updateDeletionTime(final int objectId, final Timestamp deletionDate) {
        try {
            jdbcClient.sql(UPDATE_DELETION_TIME_QUERY)
                .param(deletionDate)
                .param(objectId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.6528985a7d33", objectId), e);
        }
    }

    /**
     * 保存角色的创建时间。
     * Stores a character's creation timestamp.
     * @param objectId 玩家对象 ID / player object id
     * @param creationDate 创建时间戳 / creation timestamp
     */
    @Override
    public void storeCreationTime(final int objectId, final Timestamp creationDate) {
        try {
            jdbcClient.sql(UPDATE_CREATION_TIME_QUERY)
                .param(creationDate)
                .param(objectId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.bf6a6ac028ab", objectId), e);
        }
    }

    /**
     * 保存角色的最后在线时间。
     * Stores a character's last online timestamp.
     * @param objectId 玩家对象 ID / player object id
     * @param lastOnline 最后在线时间 / last online timestamp
     */
    @Override
    public void storeLastOnlineTime(final int objectId, final Timestamp lastOnline) {
        try {
            jdbcClient.sql(UPDATE_LAST_ONLINE_QUERY)
                .param(lastOnline)
                .param(objectId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.da8fe6970768", objectId), e);
        }
    }

    /**
     * 获取数据库中所有已使用的角色对象 ID。
     * Returns all player object IDs currently used in the database.
     * @return 已用 ID 数组；查询异常时为空数组 / array of used ids, empty on error
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
            log.error(I18n.get("log.ce9d363cd21c"), e);
            return new int[0];
        }
    }

    /**
     * 更新单个玩家的在线状态。
     * Updates a single player's online status flag.
     * @param player 玩家 / player
     * @param online 是否在线 / whether online
     */
    @Override
    public void onlinePlayer(final Player player, final boolean online) {
        try {
            log.debug("Setting online status {} {}", player.getObjectId(), player.getName());
            jdbcClient.sql(UPDATE_ONLINE_STATUS_QUERY)
                .param(online)
                .param(player.getObjectId())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.91f6d0189d5c", player.getObjectId()), e);
        }
    }

    /**
     * 批量设置全部玩家的在线状态（通常用于服务器启动/关闭）。
     * Sets the online flag for all players (typically on server start/shutdown).
     * @param online 目标在线状态 / target online flag
     */
    @Override
    public void setPlayersOffline(final boolean online) {
        try {
            jdbcClient.sql(UPDATE_ALL_ONLINE_QUERY)
                .param(online)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.d115134bf89a"), e);
        }
    }

    /**
     * 按对象 ID 查询角色名。
     * Looks up a character name by object ID.
     * @param playerObjId 玩家对象 ID / player object id
     * @return 角色名，未找到时为 null / name, or null if not found
     */
    @Override
    public String getPlayerNameByObjId(final int playerObjId) {
        try {
            return jdbcClient.sql(SELECT_PLAYER_NAME_BY_OBJ_ID_QUERY)
                .param(playerObjId)
                .query(String.class)
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.0b502d634f86", playerObjId), e);
            return null;
        }
    }

    /**
     * 按对象 ID 查询月神消费点数。
     * Looks up luna consume points by player object ID.
     * @param playerObjId 玩家对象 ID / player object id
     * @return 月神消费点数，未找到或异常时为 0 / luna consume points, or 0 if missing/error
     */
    @Override
    public int getPlayerLunaConsumeByObjId(final int playerObjId) {
        try {
            Integer val = jdbcClient.sql(SELECT_LUNA_CONSUME_BY_OBJ_ID_QUERY)
                .param(playerObjId)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.3620223b3e3a", playerObjId), e);
            return 0;
        }
    }

    /**
     * 按角色名查询对象 ID。
     * Looks up a player object ID by character name.
     * @param playerName 角色名 / character name
     * @return 对象 ID，未找到或异常时为 0 / object id, or 0 if missing/error
     */
    @Override
    public int getPlayerIdByName(final String playerName) {
        try {
            Integer val = jdbcClient.sql(SELECT_PLAYER_ID_BY_NAME)
                .param(playerName)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.f273cc47e33d", playerName), e);
            return 0;
        }
    }

    /**
     * 按角色名查询所属账号 ID。
     * Looks up the account ID for a character name.
     * @param name 角色名 / character name
     * @return 账号 ID，未找到或异常时为 0 / account id, or 0 if missing/error
     */
    @Override
    public int getAccountIdByName(final String name) {
        try {
            Integer val = jdbcClient.sql(SELECT_ACCOUNT_ID_BY_NAME_QUERY)
                .param(name)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.b4979008052c", name), e);
            return 0;
        }
    }

    /**
     * 持久化角色名变更。
     * Persists a player name change.
     * @param recipientCommonData 含新名称的公共数据 / common data holding the new name
     */
    @Override
    public void storePlayerName(final PlayerCommonData recipientCommonData) {
        try {
            log.debug("Storing playerName {} {}", recipientCommonData.getPlayerObjId(), recipientCommonData.getName());

            jdbcClient.sql(UPDATE_PLAYER_NAME_QUERY)
                .param(recipientCommonData.getName())
                .param(recipientCommonData.getPlayerObjId())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ecfd38bef3fc", recipientCommonData.getPlayerObjId(),
                    recipientCommonData.getName()), e);
        }
    }

    /**
     * 统计账号下未删除（或删除时间未到）的角色数量。
     * Counts non-deleted (or not-yet-deleted) characters on an account.
     * @param accountId 账号 ID / account id
     * @return 角色数量 / character count
     */
    @Override
    public int getCharacterCountOnAccount(final int accountId) {
        try {
            Integer val = jdbcClient.sql(SELECT_CHARACTER_COUNT_QUERY)
                .param(accountId)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.fe988f5216b0", accountId), e);
            return 0;
        }
    }

    /**
     * 按种族统计达到最低等级经验阈值的去重账号数。
     * Counts distinct accounts of a race that meet the minimum level exp threshold.
     * @param race 阵营 / race
     * @return 账号数量 / account count
     */
    @Override
    public int getCharacterCountForRace(Race race) {
        try {
            Integer val = jdbcClient.sql(SELECT_CHARACTER_COUNT_RACE_QUERY)
                .param(race.name())
                .param(DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(GSConfig.RATIO_MIN_REQUIRED_LEVEL))
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.6a4af16dff6d", race), e);
            return 0;
        }
    }

    /**
     * 统计当前标记为在线的玩家数量。
     * Counts players currently marked as online.
     * @return 在线玩家数 / online player count
     */
    @Override
    public int getOnlinePlayerCount() {
        try {
            Integer val = jdbcClient.sql(SELECT_ONLINE_PLAYER_COUNT_QUERY)
                .param(true)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7cb1629e7919"), e);
            return 0;
        }
    }

    /**
     * 查询因长时间未登录而待删除的角色 ID 列表。
     * Lists player IDs inactive long enough to be deleted.
     * @param daysOfInactivity 不活跃天数阈值 / inactivity threshold in days
     * @param limitation 最大返回条数，&lt;=0 表示不限制 / max rows, &lt;=0 for no limit
     * @return 待删除角色 ID 列表 / list of player ids to delete
     */
    @Override
    public List<Integer> getPlayersToDelete(final int daysOfInactivity, int limitation) {
        StringBuilder query = new StringBuilder(SELECT_PLAYERS_TO_DELETE_QUERY);

        if (limitation > 0) {
            query.append(" LIMIT ").append(limitation);
        }

        try {
            return jdbcClient.sql(query.toString())
                .param(daysOfInactivity)
                .query((rset, rowNum) -> rset.getInt("id"))
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e40ff813df6d"), e);
            return new ArrayList<>();
        }
    }

    /**
     * 设置玩家上次转服时间。
     * Sets a player's last server-transfer time.
     * @param playerId 玩家对象 ID / player object id
     * @param time 转服时间戳 / transfer timestamp
     */
    @Override
    public void setPlayerLastTransferTime(final int playerId, final long time) {
        try {
            jdbcClient.sql(UPDATE_LAST_TRANSFER_TIME_QUERY)
                .param(time)
                .param(playerId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.557725f5c465", playerId), e);
        }
    }

    /**
     * 查询角色创建时间。
     * Loads a character's creation timestamp.
     * @param obj 玩家对象 ID / player object id
     * @return 创建时间，未找到或异常时为 null / creation timestamp, or null if missing/error
     */
    @Override
    public Timestamp getCharacterCreationDateId(final int obj) {
        try {
            return jdbcClient.sql(SELECT_CREATION_DATE_QUERY)
                .param(obj)
                .query(Timestamp.class)
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.5725f16be2cd", obj), e);
            return null;
        }
    }

    /**
     * 更新玩家的军团加入申请状态。
     * Updates a player's legion join-request state.
     * @param playerId 玩家对象 ID / player object id
     * @param state 申请状态 / join request state
     */
    @Override
    public void updateLegionJoinRequestState(final int playerId, final LegionJoinRequestState state) {
        try {
            jdbcClient.sql(UPDATE_JOIN_STATE_QUERY)
                .param(state.name())
                .param(playerId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ae369018dad0", playerId), e);
        }
    }

    /**
     * 清除玩家的军团加入申请（军团 ID 置 0，状态置 NONE）。
     * Clears a player's legion join request (legion id 0, state NONE).
     * @param playerId 玩家对象 ID / player object id
     */
    @Override
    public void clearJoinRequest(final int playerId) {
        try {
            jdbcClient.sql(CLEAR_JOIN_REQUEST_QUERY)
                .param(0)
                .param("NONE")
                .param(playerId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.c8dcb65f0a0c", playerId), e);
        }
    }

    /**
     * 从数据库读取并写入玩家的军团加入申请状态。
     * Loads the legion join-request state from DB into the player.
     * @param player 目标玩家 / target player
     */
    @Override
    public void getJoinRequestState(final Player player) {
        try {
            jdbcClient.sql(SELECT_JOIN_STATE_QUERY)
                .param(player.getObjectId())
                .query((rset, rowNum) -> {
                    try {
                        player.getCommonData().setJoinRequestState(
                            LegionJoinRequestState.valueOf(rset.getString("join_state")));
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.2ab35f9897c1", player.getObjectId(), rset.getString("join_state")));
                        player.getCommonData().setJoinRequestState(LegionJoinRequestState.NONE);
                    }
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.01aadaeb4af5", player.getObjectId()), e);
        }
    }

    /**
     * 判断当前 DAO 是否支持给定数据库版本。
     * Reports whether this DAO supports the given database version.
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
