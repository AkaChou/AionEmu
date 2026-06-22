package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.configs.main.CacheConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerDAO;
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
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;
import com.google.common.collect.Maps;
import javolution.util.FastMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * MySQL 8 implementation of PlayerDAO
 * Fixed connection leaks - removed all DB.insertUpdate() and DB.prepareStatement()
 */
public class MySQL8PlayerDAO extends PlayerDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerDAO.class);
    
    private FastMap<Integer, PlayerCommonData> playerCommonData = new FastMap<Integer, PlayerCommonData>().shared();
    private FastMap<String, PlayerCommonData> playerCommonDataByName = new FastMap<String, PlayerCommonData>().shared();

    // Queries
    private static final String CHECK_NAME_USED_QUERY = "SELECT COUNT(id) as cnt FROM players WHERE name = ?";
    private static final String SELECT_PLAYER_NAMES_QUERY = "SELECT id, `name` FROM players WHERE id IN (%s)";
    private static final String UPDATE_PLAYER_QUERY = "UPDATE players SET " +  "name = ?, exp = ?, recoverexp = ?, x = ?, y = ?, z = ?, heading = ?, " + "world_id = ?, gender = ?, race = ?, player_class = ?, last_online = ?, " + "quest_expands = ?, npc_expands = ?, advenced_stigma_slot_size = ?, " + "warehouse_size = ?, note = ?, title_id = ?, bonus_title_id = ?, " + "dp = ?, soul_sickness = ?, mailbox_letters = ?, reposte_energy = ?, " + "mentor_flag_time = ?, world_owner = ?, stamps = ?, rewarded_pass = ?, " + "last_stamp = ?, passport_time = ?, is_archdaeva = ?, creativity_point = ?, " + "aura_of_growth = ?, join_legion_id = ?, join_state = ?, berdin_star = ?, " + "abyss_favor = ?, luna_consume = ?, muni_keys = ?, luna_consume_count = ?, " + "wardrobe_slot = ?, frenzy_points = ?, frenzy_count = ?, toc_floor = ?, " + "stone_cp = ?, golden_dice = ?, sweep_reset = ?, minion_skill_points = ?, " + "minion_function_time = ? WHERE id = ?";
    
    private static final String INSERT_PLAYER_QUERY = "INSERT INTO players " + "(id, `name`, account_id, account_name, x, y, z, heading, " + "world_id, gender, race, player_class, quest_expands, npc_expands, " + "warehouse_size, bonus_title_id, is_archdaeva, wardrobe_slot, " + "online, stamps, rewarded_pass) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 2, 0, 0, 1)";
    
    private static final String SELECT_PLAYER_ID_BY_NAME_QUERY = "SELECT id FROM players WHERE name = ?";
    private static final String SELECT_PLAYER_COMMON_DATA_QUERY = "SELECT * FROM players WHERE id = ?";
    private static final String DELETE_PLAYER_QUERY = "DELETE FROM players WHERE id = ?";
    private static final String SELECT_PLAYER_OIDS_ON_ACCOUNT_QUERY = "SELECT id FROM players WHERE account_id = ?";
    private static final String SELECT_CREATION_DELETION_TIME_QUERY = "SELECT creation_date, deletion_date FROM players WHERE id = ?";
    private static final String UPDATE_DELETION_TIME_QUERY = "UPDATE players SET deletion_date = ? WHERE id = ?";
    private static final String UPDATE_CREATION_TIME_QUERY = "UPDATE players SET creation_date = ? WHERE id = ?";
    private static final String UPDATE_LAST_ONLINE_QUERY = "UPDATE players SET last_online = ? WHERE id = ?";
    private static final String SELECT_USED_IDS_QUERY = "SELECT id FROM players";
    private static final String UPDATE_ONLINE_STATUS_QUERY = "UPDATE players SET online = ? WHERE id = ?";
    private static final String UPDATE_ALL_ONLINE_QUERY = "UPDATE players SET online = ?";
    private static final String SELECT_PLAYER_NAME_BY_OBJ_ID_QUERY = "SELECT name FROM players WHERE id = ?";
    private static final String SELECT_LUNA_CONSUME_BY_OBJ_ID_QUERY = "SELECT luna_consume FROM players WHERE id = ?";
    private static final String SELECT_PLAYER_ID_BY_NAME = "SELECT id FROM players WHERE name = ?";
    private static final String SELECT_ACCOUNT_ID_BY_NAME_QUERY = "SELECT `account_id` FROM `players` WHERE `name` = ?";
    private static final String UPDATE_PLAYER_NAME_QUERY = "UPDATE players SET name = ? WHERE id = ?";
    private static final String SELECT_CHARACTER_COUNT_QUERY = "SELECT COUNT(*) AS cnt FROM `players` WHERE `account_id` = ? " + "AND (deletion_date IS NULL OR deletion_date > CURRENT_TIMESTAMP)";
    private static final String SELECT_CHARACTER_COUNT_RACE_QUERY = "SELECT COUNT(DISTINCT(`account_name`)) AS `count` FROM `players` " + "WHERE `race` = ? AND `exp` >= ?";
    private static final String SELECT_ONLINE_PLAYER_COUNT_QUERY = "SELECT COUNT(*) AS `count` FROM `players` WHERE `online` = ?";
    private static final String SELECT_PLAYERS_TO_DELETE_QUERY = "SELECT id FROM players WHERE UNIX_TIMESTAMP(CURDATE()) - " + "UNIX_TIMESTAMP(last_online) > ? * 24 * 60 * 60";
    private static final String UPDATE_LAST_TRANSFER_TIME_QUERY = "UPDATE players SET last_transfer_time = ? WHERE id = ?";
    private static final String SELECT_CREATION_DATE_QUERY = "SELECT `creation_date` FROM `players` WHERE `id` = ?";
    private static final String UPDATE_JOIN_STATE_QUERY = "UPDATE players SET join_state = ? WHERE id = ?";
    private static final String CLEAR_JOIN_REQUEST_QUERY = "UPDATE players SET join_legion_id = ?, join_state = ? WHERE id = ?";
    private static final String SELECT_JOIN_STATE_QUERY = "SELECT join_state FROM players WHERE id = ?";

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
            log.error("Can't check if name '{}' is used", name, e);
            return true;
        }
    }

    @Override
    public Map<Integer, String> getPlayerNames(Collection<Integer> playerObjectIds) {
        if (GenericValidator.isBlankOrNull(playerObjectIds)) {
            return Collections.emptyMap();
        }
        
        Map<Integer, String> result = Maps.newHashMap();
        String sql = String.format(SELECT_PLAYER_NAMES_QUERY, StringUtils.join(playerObjectIds, ", "));
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(sql);
             ResultSet rs = s.executeQuery()) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                result.put(id, name);
            }
        } catch (SQLException e) {
            log.error("Failed to load player names", e);
        }
        
        return result;
    }

    @Override
    public void storePlayer(final Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_PLAYER_QUERY)) {
            
            log.debug("Storing player {} {}", player.getObjectId(), player.getName());
            PlayerCommonData pcd = player.getCommonData();
            
            stmt.setString(1, player.getName());
            stmt.setLong(2, pcd.getExp());
            stmt.setLong(3, pcd.getExpRecoverable());
            stmt.setFloat(4, player.getX());
            stmt.setFloat(5, player.getY());
            stmt.setFloat(6, player.getZ());
            stmt.setInt(7, player.getHeading());
            stmt.setInt(8, player.getWorldId());
            stmt.setString(9, player.getGender().toString());
            stmt.setString(10, player.getRace().toString());
            stmt.setString(11, pcd.getPlayerClass().toString());
            stmt.setTimestamp(12, pcd.getLastOnline());
            stmt.setInt(13, player.getQuestExpands());
            stmt.setInt(14, player.getNpcExpands());
            stmt.setInt(15, pcd.getAdvancedStigmaSlotSize());
            stmt.setInt(16, player.getWarehouseSize());
            stmt.setString(17, pcd.getNote());
            stmt.setInt(18, pcd.getTitleId());
            stmt.setInt(19, pcd.getBonusTitleId());
            stmt.setInt(20, pcd.getDp());
            stmt.setInt(21, pcd.getDeathCount());
            
            Mailbox mailBox = player.getMailbox();
            int mails = mailBox != null ? mailBox.size() : pcd.getMailboxLetters();
            stmt.setInt(22, mails);
            
            stmt.setLong(23, pcd.getCurrentReposteEnergy());
            stmt.setInt(24, pcd.getMentorFlagTime());
            stmt.setInt(25, player.getPosition().getWorldMapInstance().getOwnerId());
            stmt.setInt(26, pcd.getPassportStamps());
            stmt.setInt(27, pcd.getPassportReward());
            stmt.setTimestamp(28, pcd.getLastStamp());
            stmt.setInt(29, pcd.getPassportTime());
            stmt.setBoolean(30, pcd.isArchDaeva());
            stmt.setInt(31, pcd.getCreativityPoint());
            stmt.setLong(32, pcd.getAuraOfGrowth());
            stmt.setInt(33, pcd.getJoinRequestLegionId());
            stmt.setString(34, pcd.getJoinRequestState().toString());
            stmt.setLong(35, pcd.getBerdinStar());
            stmt.setLong(36, pcd.getAbyssFavor());
            stmt.setInt(37, pcd.getLunaConsumePoint());
            stmt.setInt(38, pcd.getMuniKeys());
            stmt.setInt(39, pcd.getLunaConsumeCount());
            stmt.setInt(40, pcd.getWardrobeSlot());
            
            PlayerUpgradeArcade pua = player.getUpgradeArcade();
            stmt.setInt(41, pua != null ? pua.getFrenzyPoints() : 0);
            stmt.setInt(42, pua != null ? pua.getFrenzyCount() : 0);
            
            stmt.setInt(43, pcd.getFloor());
            stmt.setInt(44, pcd.getStoneCreativityPoint());
            stmt.setInt(45, pcd.getGoldenDice());
            stmt.setInt(46, pcd.getResetBoard());
            stmt.setInt(47, pcd.getMinionSkillPoints());
            stmt.setTimestamp(48, pcd.getMinionFunctionTime());
            stmt.setInt(49, player.getObjectId());
            
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error saving player: {} {}", player.getObjectId(), player.getName(), e);
        }
        
        if (CacheConfig.CACHE_COMMONDATA) {
            playerCommonData.put(player.getObjectId(), player.getCommonData());
            playerCommonDataByName.put(player.getName().toLowerCase(), player.getCommonData());
        }
    }

    @Override
    public boolean saveNewPlayer(final PlayerCommonData pcd, final int accountId, final String accountName) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_PLAYER_QUERY)) {
            
            log.debug("Saving new player: {} {}", pcd.getPlayerObjId(), pcd.getName());
            
            stmt.setInt(1, pcd.getPlayerObjId());
            stmt.setString(2, pcd.getName());
            stmt.setInt(3, accountId);
            stmt.setString(4, accountName);
            stmt.setFloat(5, pcd.getPosition().getX());
            stmt.setFloat(6, pcd.getPosition().getY());
            stmt.setFloat(7, pcd.getPosition().getZ());
            stmt.setInt(8, pcd.getPosition().getHeading());
            stmt.setInt(9, pcd.getPosition().getMapId());
            stmt.setString(10, pcd.getGender().toString());
            stmt.setString(11, pcd.getRace().toString());
            stmt.setString(12, pcd.getPlayerClass().toString());
            stmt.setInt(13, pcd.getQuestExpands());
            stmt.setInt(14, pcd.getNpcExpands());
            stmt.setInt(15, pcd.getWarehouseSize());
            stmt.setInt(16, pcd.getBonusTitleId());
            stmt.setBoolean(17, pcd.isArchDaeva());
            stmt.executeUpdate();
            
            if (CacheConfig.CACHE_COMMONDATA) {
                playerCommonData.put(pcd.getPlayerObjId(), pcd);
                playerCommonDataByName.put(pcd.getName().toLowerCase(), pcd);
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error saving new player: {} {}", pcd.getPlayerObjId(), pcd.getName(), e);
            return false;
        }
    }

    @Override
    public PlayerCommonData loadPlayerCommonDataByName(final String name) {
        Player player = World.getInstance().findPlayer(name);
        if (player != null) {
            return player.getCommonData();
        }
        
        PlayerCommonData pcd = playerCommonDataByName.get(name.toLowerCase());
        if (pcd != null) {
            return pcd;
        }
        
        int playerObjId = 0;
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_ID_BY_NAME_QUERY)) {
            
            stmt.setString(1, name);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    playerObjId = rset.getInt("id");
                }
            }
        } catch (Exception e) {
            log.error("Could not get player ID for name: {}", name, e);
        }
        
        return playerObjId > 0 ? loadPlayerCommonData(playerObjId) : null;
    }

    @Override
    public PlayerCommonData loadPlayerCommonData(final int playerObjId) {
        PlayerCommonData cached = playerCommonData.get(playerObjId);
        if (cached != null) {
            log.debug("PlayerCommonData for id: {} obtained from cache", playerObjId);
            return cached;
        }
        
        final PlayerCommonData cd = new PlayerCommonData(playerObjId);
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_COMMON_DATA_QUERY)) {
            
            stmt.setInt(1, playerObjId);
            log.debug("Loading player from db: {}", playerObjId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    cd.setName(resultSet.getString("name"));
                    
                    try {
                        cd.setPlayerClass(PlayerClass.valueOf(resultSet.getString("player_class")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid player class for player {}: {}", playerObjId, resultSet.getString("player_class"));
                        cd.setPlayerClass(PlayerClass.WARRIOR);
                    }
                    
                    cd.setExp(resultSet.getLong("exp"), false);
                    cd.setRecoverableExp(resultSet.getLong("recoverexp"));
                    
                    try {
                        cd.setRace(Race.valueOf(resultSet.getString("race")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid race for player {}: {}", playerObjId, resultSet.getString("race"));
                        cd.setRace(Race.ELYOS);
                    }
                    
                    try {
                        cd.setGender(Gender.valueOf(resultSet.getString("gender")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid gender for player {}: {}", playerObjId, resultSet.getString("gender"));
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
                    MapRegion mr = World.getInstance().getWorldMap(worldId).getMainWorldMapInstance().getRegion(x, y, z);
                    
                    if (mr == null && playerInitialData != null) {
                        LocationData ld = playerInitialData.getSpawnLocation(cd.getRace());
                        if (ld != null) {
                            x = ld.getX();
                            y = ld.getY();
                            z = ld.getZ();
                            heading = ld.getHeading();
                            worldId = ld.getMapId();
                        }
                    }
                    
                    WorldPosition position = World.getInstance().createPosition(worldId, x, y, z, heading, 0);
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
                        log.warn("Invalid join state for player {}: {}", playerObjId, resultSet.getString("join_state"));
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
                    
                    return cd;
                }
            }
        } catch (Exception e) {
            log.error("Could not restore PlayerCommonData for player: {}", playerObjId, e);
        }
        
        return null;
    }

    @Override
    public void deletePlayer(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(DELETE_PLAYER_QUERY)) {
            
            statement.setInt(1, playerId);
            statement.executeUpdate();
            
            if (CacheConfig.CACHE_COMMONDATA) {
                PlayerCommonData pcd = playerCommonData.remove(playerId);
                if (pcd != null) {
                    playerCommonDataByName.remove(pcd.getName().toLowerCase());
                }
            }
        } catch (SQLException e) {
            log.error("Error deleting player: {}", playerId, e);
        }
    }

    @Override
    public List<Integer> getPlayerOidsOnAccount(final int accountId) {
        final List<Integer> result = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_OIDS_ON_ACCOUNT_QUERY)) {
            
            stmt.setInt(1, accountId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    result.add(resultSet.getInt("id"));
                }
            }
        } catch (Exception e) {
            log.error("Error getting player OIDs for account: {}", accountId, e);
            return null;
        }
        
        return result;
    }

    @Override
    public void setCreationDeletionTime(final PlayerAccountData acData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_CREATION_DELETION_TIME_QUERY)) {
            
            stmt.setInt(1, acData.getPlayerCommonData().getPlayerObjId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    acData.setDeletionDate(rset.getTimestamp("deletion_date"));
                    acData.setCreationDate(rset.getTimestamp("creation_date"));
                }
            }
        } catch (Exception e) {
            log.error("Error getting creation/deletion time for player: {}", 
                acData.getPlayerCommonData().getPlayerObjId(), e);
        }
    }

    @Override
    public void updateDeletionTime(final int objectId, final Timestamp deletionDate) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_DELETION_TIME_QUERY)) {
            
            stmt.setTimestamp(1, deletionDate);
            stmt.setInt(2, objectId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error updating deletion time for player: {}", objectId, e);
        }
    }

    @Override
    public void storeCreationTime(final int objectId, final Timestamp creationDate) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_CREATION_TIME_QUERY)) {
            
            stmt.setTimestamp(1, creationDate);
            stmt.setInt(2, objectId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error storing creation time for player: {}", objectId, e);
        }
    }

    @Override
    public void storeLastOnlineTime(final int objectId, final Timestamp lastOnline) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LAST_ONLINE_QUERY)) {
            
            stmt.setTimestamp(1, lastOnline);
            stmt.setInt(2, objectId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error storing last online time for player: {}", objectId, e);
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
            log.error("Can't get list of id's from players table", e);
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    @Override
    public void onlinePlayer(final Player player, final boolean online) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ONLINE_STATUS_QUERY)) {
            
            log.debug("Setting online status {} {}", player.getObjectId(), player.getName());
            stmt.setBoolean(1, online);
            stmt.setInt(2, player.getObjectId());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error setting online status for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public void setPlayersOffline(final boolean online) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ALL_ONLINE_QUERY)) {
            
            stmt.setBoolean(1, online);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error setting all players offline status", e);
        }
    }

    @Override
    public String getPlayerNameByObjId(final int playerObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_NAME_BY_OBJ_ID_QUERY)) {
            
            stmt.setInt(1, playerObjId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (Exception e) {
            log.error("Error getting player name by objId: {}", playerObjId, e);
        }
        
        return null;
    }
    
    @Override
    public int getPlayerLunaConsumeByObjId(final int playerObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LUNA_CONSUME_BY_OBJ_ID_QUERY)) {
            
            stmt.setInt(1, playerObjId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("luna_consume");
                }
            }
        } catch (Exception e) {
            log.error("Error getting luna consume by objId: {}", playerObjId, e);
        }
        
        return 0;
    }
    
    @Override
    public int getPlayerIdByName(final String playerName) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_ID_BY_NAME)) {
            
            stmt.setString(1, playerName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            log.error("Error getting player ID by name: {}", playerName, e);
        }
        
        return 0;
    }

    @Override
    public int getAccountIdByName(final String name) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(SELECT_ACCOUNT_ID_BY_NAME_QUERY)) {
            
            s.setString(1, name);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("account_id");
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Error getting account ID for player: {}", name, e);
            return 0;
        }
    }

    @Override
    public void storePlayerName(final PlayerCommonData recipientCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_PLAYER_NAME_QUERY)) {
            
            log.debug("Storing playerName {} {}", recipientCommonData.getPlayerObjId(), recipientCommonData.getName());
            
            stmt.setString(1, recipientCommonData.getName());
            stmt.setInt(2, recipientCommonData.getPlayerObjId());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error saving playerName: {} {}", recipientCommonData.getPlayerObjId(), recipientCommonData.getName(), e);
        }
    }

    @Override
    public int getCharacterCountOnAccount(final int accountId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_CHARACTER_COUNT_QUERY)) {
            
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Error getting character count for account: {}", accountId, e);
            return 0;
        }
    }

    @Override
    public int getCharacterCountForRace(Race race) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_CHARACTER_COUNT_RACE_QUERY)) {
            
            stmt.setString(1, race.name());
            stmt.setLong(2, DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(GSConfig.RATIO_MIN_REQUIRED_LEVEL));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Error getting character count for race: {}", race, e);
            return 0;
        }
    }

    @Override
    public int getOnlinePlayerCount() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ONLINE_PLAYER_COUNT_QUERY)) {
            
            stmt.setBoolean(1, true);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("Error getting online player count", e);
            return 0;
        }
    }

    @Override
    public List<Integer> getPlayersToDelete(final int daysOfInactivity, int limitation) {
        StringBuilder query = new StringBuilder(SELECT_PLAYERS_TO_DELETE_QUERY);
        
        if (limitation > 0) {
            query.append(" LIMIT ").append(limitation);
        }
        
        final List<Integer> playersToDelete = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query.toString())) {
            
            stmt.setInt(1, daysOfInactivity);
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    playersToDelete.add(rset.getInt("id"));
                }
            }
        } catch (Exception e) {
            log.error("Error getting players to delete", e);
        }
        
        return playersToDelete;
    }

    @Override
    public void setPlayerLastTransferTime(final int playerId, final long time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LAST_TRANSFER_TIME_QUERY)) {
            
            stmt.setLong(1, time);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error setting last transfer time for player: {}", playerId, e);
        }
    }
    
    @Override
    public Timestamp getCharacterCreationDateId(final int obj) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(SELECT_CREATION_DATE_QUERY)) {
            
            s.setInt(1, obj);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("creation_date");
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting creation date for player: {}", obj, e);
            return null;
        }
    }
    
    @Override
    public void updateLegionJoinRequestState(final int playerId, final LegionJoinRequestState state) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_JOIN_STATE_QUERY)) {
            
            stmt.setString(1, state.name());
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error updating join request state for player: {}", playerId, e);
        }
    }
    
    @Override
    public void clearJoinRequest(final int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CLEAR_JOIN_REQUEST_QUERY)) {
            
            stmt.setInt(1, 0);
            stmt.setString(2, "NONE");
            stmt.setInt(3, playerId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error clearing join request for player: {}", playerId, e);
        }
    }
    
    @Override
    public void getJoinRequestState(final Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_JOIN_STATE_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    try {
                        player.getCommonData().setJoinRequestState(
                            LegionJoinRequestState.valueOf(rset.getString("join_state")));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid join state for player {}: {}", player.getObjectId(), rset.getString("join_state"));
                        player.getCommonData().setJoinRequestState(LegionJoinRequestState.NONE);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting join request state for player: {}", player.getObjectId(), e);
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}