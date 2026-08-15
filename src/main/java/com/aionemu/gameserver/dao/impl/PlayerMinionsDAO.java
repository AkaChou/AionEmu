package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家随从（Minion）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerMinionsDAO.
 *
 * @author Falke_34
 * Updated for MySQL 8 support
 */
@Slf4j
public class PlayerMinionsDAO extends com.aionemu.gameserver.dao.PlayerMinionsDAO {


    /** 插入玩家随从 / Insert player minion */
    private static final String INSERT_QUERY = "INSERT INTO player_minions (player_id, object_id, minion_id, name, grade, level, growthpoints, birthday, is_locked, buff_bag) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 0, '')";
    /** 删除玩家随从 / Delete player minion */
    private static final String DELETE_QUERY = "DELETE FROM player_minions WHERE player_id = ? AND object_id = ?";
    /** 查询玩家随从列表 / Select player minions */
    private static final String SELECT_QUERY = "SELECT * FROM player_minions WHERE player_id = ?";
    /** 更新随从名称 / Update minion name */
    private static final String UPDATE_NAME_QUERY = "UPDATE player_minions SET name = ? WHERE player_id = ? AND object_id = ?";
    /** 更新随从成长点 / Update minion growth points */
    private static final String UPDATE_GROWTH_QUERY = "UPDATE player_minions SET growthpoints = ? WHERE player_id = ? AND object_id = ?";
    /** 随从进化 / Evolve minion */
    private static final String EVOLUTION_QUERY = "UPDATE player_minions SET minion_id = ?, growthpoints = 0, level = ? WHERE player_id = ? AND object_id = ?";
    /** 锁定/解锁随从 / Lock or unlock minion */
    private static final String LOCK_QUERY = "UPDATE player_minions SET is_locked = ? WHERE player_id = ? AND object_id = ?";
    /** 更新随从增益背包 / Update minion doping bag */
    private static final String UPDATE_DOPING_QUERY = "UPDATE player_minions SET buff_bag = ? WHERE player_id = ? AND object_id = ?";
    /** 查询随从生日 / Select minion birthday */
    private static final String SELECT_BIRTHDAY_QUERY = "SELECT birthday FROM player_minions WHERE player_id = ? AND object_id = ?";
    /** 清理孤儿随从数据 / Clean orphaned minion rows */
    private static final String CLEAN_ORPHANED_QUERY = "DELETE FROM player_minions WHERE player_id NOT IN (SELECT id FROM players)";

    /**
     * 插入新的玩家随从记录。
     * Inserts a new player minion record.
     *
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean insertPlayerMinion(MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection()) {
            return insertMinion(con, minionCommonData);
        } catch (Exception e) {
            log.error(I18n.get("log.2527877e3b8c", minionCommonData.getMinionId(), minionCommonData.getName(), e), e);
            return false;
        }
    }

    private static boolean insertMinion(Connection con, MinionCommonData minionCommonData) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            stmt.setInt(1, minionCommonData.getMasterObjectId());
            stmt.setInt(2, minionCommonData.getObjectId());
            stmt.setInt(3, minionCommonData.getMinionId());
            stmt.setString(4, minionCommonData.getName());
            stmt.setString(5, minionCommonData.getMinionGrade());
            stmt.setInt(6, minionCommonData.getMinionLevel());
            stmt.setInt(7, minionCommonData.getMinionGrowthPoint());
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * 移除玩家的指定随从。
     * Removes the specified minion for the player.
     *
     * @param player 玩家 / player
     * @param minionObjectId 仆从对象 ID / minion object id
     */
    @Override
    public boolean removePlayerMinion(Player player, int minionObjectId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, minionObjectId);
            return stmt.executeUpdate() == 1;

        } catch (Exception e) {
            log.error(I18n.get("log.85f2880b076f", minionObjectId, e), e);
            return false;
        }
    }

    /**
     * 加载玩家的全部随从数据。
     * Loads all minions belonging to the player.
     *
     * @param player 玩家 / player
     * @return 仆从列表 / minion list
     */
    @Override
    public List<MinionCommonData> getPlayerMinions(Player player) {
        List<MinionCommonData> minions = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MinionCommonData minionCommonData = new MinionCommonData(
                        rs.getInt("object_id"),
                        rs.getInt("minion_id"),
                        player.getObjectId(),
                        rs.getString("name"),
                        rs.getString("grade"),
                        rs.getInt("level"),
                        rs.getInt("growthpoints")
                    );

                    minionCommonData.setBirthday(rs.getTimestamp("birthday"));
                    minionCommonData.setLock(rs.getInt("is_locked") == 1);

                    int minionId = rs.getInt("minion_id");
                    if (minionId > 980013) {
                        MinionDopingBag dopingBag = minionCommonData.getDopingBag();
                        if (dopingBag != null) {
                            String bag = rs.getString("buff_bag");
                            if (bag != null && !bag.isEmpty()) {
                                String[] ids = bag.split(",");
                                for (int i = 0; i < ids.length; i++) {
                                    if (i < 6 && !ids[i].isEmpty()) {
                                        try {
                                            int itemId = Integer.parseInt(ids[i]);
                                            dopingBag.setItem(itemId, i);
                                        } catch (NumberFormatException e) {
                                            log.warn(I18n.get("log.e8efb9fcad1f", minionCommonData.getObjectId(), ids[i]));
                                        }
                                    }
                                }
                            }
                        }
                    }

                    minions.add(minionCommonData);
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.6b0198f98c6b", player.getObjectId(), e), e);
            return null;
        }

        return minions;
    }

    /**
     * 更新随从名称。
     * Updates the minion name.
     *
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean updateMinionName(MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_NAME_QUERY)) {

            stmt.setString(1, minionCommonData.getName());
            stmt.setInt(2, minionCommonData.getMasterObjectId());
            stmt.setInt(3, minionCommonData.getObjectId());
            return stmt.executeUpdate() == 1;

        } catch (Exception e) {
            log.error(I18n.get("log.d5697671e606", minionCommonData.getMinionId(), e), e);
            return false;
        }
    }

    /**
     * 更新玩家随从的成长点。
     * Updates the growth points of a player minion.
     *
     * @param player 玩家 / player
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean updatePlayerMinionGrowthPoint(Player player, MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_GROWTH_QUERY)) {

            stmt.setInt(1, minionCommonData.getMinionGrowthPoint());
            stmt.setInt(2, minionCommonData.getMasterObjectId());
            stmt.setInt(3, minionCommonData.getObjectId());
            return stmt.executeUpdate() == 1;

        } catch (Exception e) {
            log.error(I18n.get("log.d7a903023f02", minionCommonData.getMinionId(), e), e);
            return false;
        }
    }

    @Override
    public boolean updateGrowthAndRemoveMaterials(Player player, MinionCommonData minionCommonData,
            List<Integer> materialObjectIds) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!updateGrowth(con, player.getObjectId(), minionCommonData)) {
                    throw new SQLException("minion growth target was not updated");
                }
                deleteMaterials(con, player.getObjectId(), materialObjectIds);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error(I18n.get("log.d7a903023f02", minionCommonData.getMinionId(), e), e);
            return false;
        }
    }

    private static boolean updateGrowth(Connection con, int playerId, MinionCommonData minionCommonData) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_GROWTH_QUERY)) {
            stmt.setInt(1, minionCommonData.getMinionGrowthPoint());
            stmt.setInt(2, playerId);
            stmt.setInt(3, minionCommonData.getObjectId());
            return stmt.executeUpdate() == 1;
        }
    }

    private static void deleteMaterials(Connection con, int playerId, List<Integer> materialObjectIds) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            for (int materialObjectId : materialObjectIds) {
                stmt.setInt(1, playerId);
                stmt.setInt(2, materialObjectId);
                if (stmt.executeUpdate() != 1) {
                    throw new SQLException("minion material was not deleted: " + materialObjectId);
                }
            }
        }
    }

    @Override
    public boolean replacePlayerMinions(MinionCommonData replacement, List<Integer> materialObjectIds) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!insertMinion(con, replacement)) {
                    throw new SQLException("combined minion was not inserted");
                }
                deleteMaterials(con, replacement.getMasterObjectId(), materialObjectIds);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (Exception e) {
            log.error(I18n.get("log.2527877e3b8c", replacement.getMinionId(), replacement.getName(), e), e);
            return false;
        }
    }

    /**
     * 执行随从进化（更新 minion_id 与 level，重置成长点）。
     * Evolves a minion (updates minion_id and level, resets growth points).
     *
     * @param player 玩家 / player
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean evolutionMinion(Player player, MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(EVOLUTION_QUERY)) {

            stmt.setInt(1, minionCommonData.getMinionId());
            stmt.setInt(2, minionCommonData.getMinionLevel());
            stmt.setInt(3, minionCommonData.getMasterObjectId());
            stmt.setInt(4, minionCommonData.getObjectId());
            return stmt.executeUpdate() == 1;

        } catch (Exception e) {
            log.error(I18n.get("log.188874aa8e51", minionCommonData.getMinionId(), e), e);
            return false;
        }
    }

    /**
     * 锁定或解锁指定随从。
     * Locks or unlocks the specified minion.
     *
     * @param player 玩家 / player
     * @param minionObjId 仆从对象 ID / minion object id
     * @param isLocked 锁定标记 / lock flag
     */
    @Override
    public boolean lockMinions(Player player, int minionObjId, int isLocked) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOCK_QUERY)) {

            stmt.setInt(1, isLocked);
            stmt.setInt(2, player.getObjectId());
            stmt.setInt(3, minionObjId);
            return stmt.executeUpdate() == 1;

        } catch (Exception e) {
            log.error(I18n.get("log.65bb4c6328f8", minionObjId, e), e);
            return false;
        }
    }

    /**
     * 保存随从的增益（doping）背包内容。
     * Saves the minion doping bag contents.
     *
     * @param player 玩家 / player
     * @param minionCommonData 随从公共数据 / minion common data
     * @param bag 增益背包 / doping bag
     */
    @Override
    public boolean saveDopingBag(Player player, MinionCommonData minionCommonData, MinionDopingBag bag) {
        if (bag == null) {
            log.warn(I18n.get("log.342c6a28af85", minionCommonData.getObjectId()));
            return false;
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_DOPING_QUERY)) {

            StringBuilder itemIds = new StringBuilder();

            itemIds.append(bag.getFoodItem() > 0 ? bag.getFoodItem() : "0");
            itemIds.append(",");
            itemIds.append(bag.getDrinkItem() > 0 ? bag.getDrinkItem() : "0");

            for (int itemId : bag.getScrollsUsed()) {
                itemIds.append(",").append(itemId > 0 ? itemId : "0");
            }

            stmt.setString(1, itemIds.toString());
            stmt.setInt(2, player.getObjectId());
            stmt.setInt(3, minionCommonData.getObjectId());
            return stmt.executeUpdate() == 1;

        } catch (Exception e) {
            log.error(I18n.get("log.0c8cc68553ee", minionCommonData.getObjectId(), e), e);
            return false;
        }
    }

    @Override
    public int[] getUsedIDs() {
        List<Integer> ids = new ArrayList<>();
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT object_id FROM player_minions");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.6b0198f98c6b", 0, e), e);
            return new int[0];
        }
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 从数据库读取并写回随从生日时间戳。
     * Loads the minion birthday timestamp from the database into the model.
     *
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public void saveBirthday(MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_BIRTHDAY_QUERY)) {

            stmt.setInt(1, minionCommonData.getMasterObjectId());
            stmt.setInt(2, minionCommonData.getObjectId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    minionCommonData.setBirthday(rs.getTimestamp("birthday"));
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.8896cfb9d7f2", minionCommonData.getMasterObjectId(), e), e);
        }
    }

    /**
     * 清理所属玩家已不存在的孤儿随从记录。
     * Cleans orphaned minion rows whose owner player no longer exists.
     *
     * @return 删除行数 / deleted row count
     */
    public int cleanOrphanedMinions() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CLEAN_ORPHANED_QUERY)) {

            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                log.info(I18n.get("log.51adbf82f7b2", deleted));
            }
            return deleted;
        } catch (Exception e) {
            log.error(I18n.get("log.55b90eeb49ae", e), e);
            return 0;
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     *
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
