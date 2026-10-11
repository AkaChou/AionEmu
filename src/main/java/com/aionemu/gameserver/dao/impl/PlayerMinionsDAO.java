package com.aionemu.gameserver.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家随从（Minion）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerMinionsDAO using JdbcClient.
 * @author Falke_34
 * Updated for MySQL 8 support
 */
@Slf4j
@Repository
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

    private final JdbcClient jdbcClient;
    private final TransactionTemplate transactionTemplate;

    public PlayerMinionsDAO() {
        this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getTransactionTemplate());
    }

    @Autowired
    public PlayerMinionsDAO(JdbcClient jdbcClient, TransactionTemplate transactionTemplate) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate must not be null");
    }

    public PlayerMinionsDAO(JdbcClient jdbcClient) {
        this(jdbcClient, DatabaseFactory.getTransactionTemplate());
    }

    /**
     * 插入新的玩家随从记录。
     * Inserts a new player minion record.
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean insertPlayerMinion(MinionCommonData minionCommonData) {
        try {
            return jdbcClient.sql(INSERT_QUERY)
                .param(minionCommonData.getMasterObjectId())
                .param(minionCommonData.getObjectId())
                .param(minionCommonData.getMinionId())
                .param(minionCommonData.getName())
                .param(minionCommonData.getMinionGrade())
                .param(minionCommonData.getMinionLevel())
                .param(minionCommonData.getMinionGrowthPoint())
                .update() == 1;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2527877e3b8c", minionCommonData.getMinionId(), minionCommonData.getName()), e);
            return false;
        }
    }

    /**
     * 移除玩家的指定随从。
     * Removes the specified minion for the player.
     * @param player 玩家 / player
     * @param minionObjectId 仆从对象 ID / minion object id
     */
    @Override
    public boolean removePlayerMinion(Player player, int minionObjectId) {
        try {
            return jdbcClient.sql(DELETE_QUERY)
                .param(player.getObjectId())
                .param(minionObjectId)
                .update() == 1;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.85f2880b076f", minionObjectId), e);
            return false;
        }
    }

    /**
     * 加载玩家的全部随从数据。
     * Loads all minions belonging to the player.
     * @param player 玩家 / player
     * @return 仆从列表 / minion list
     */
    @Override
    public List<MinionCommonData> getPlayerMinions(Player player) {
        List<MinionCommonData> minions ;

        try {
            minions = jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rs, _) -> {
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

                    return minionCommonData;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.6b0198f98c6b", player.getObjectId()), e);
            return null;
        }

        return minions;
    }

    /**
     * 更新随从名称。
     * Updates the minion name.
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean updateMinionName(MinionCommonData minionCommonData) {
        try {
            return jdbcClient.sql(UPDATE_NAME_QUERY)
                .param(minionCommonData.getName())
                .param(minionCommonData.getMasterObjectId())
                .param(minionCommonData.getObjectId())
                .update() == 1;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.d5697671e606", minionCommonData.getMinionId()), e);
            return false;
        }
    }

    /**
     * 更新玩家随从的成长点。
     * Updates the growth points of a player minion.
     * @param player 玩家 / player
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean updateGrowthAndRemoveMaterials(Player player, MinionCommonData minionCommonData,
            List<Integer> materialObjectIds) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                int updated = jdbcClient.sql(UPDATE_GROWTH_QUERY)
                    .param(minionCommonData.getMinionGrowthPoint())
                    .param(player.getObjectId())
                    .param(minionCommonData.getObjectId())
                    .update();
                if (updated != 1) {
                    status.setRollbackOnly();
                    return false;
                }
                if (materialObjectIds != null) {
                    for (int materialObjectId : materialObjectIds) {
                        int deleted = jdbcClient.sql(DELETE_QUERY)
                            .param(player.getObjectId())
                            .param(materialObjectId)
                            .update();
                        if (deleted != 1) {
                            status.setRollbackOnly();
                            return false;
                        }
                    }
                }
                return true;
            }));
        } catch (RuntimeException e) {
            log.error(I18n.get("log.d7a903023f02", minionCommonData.getMinionId()), e);
            return false;
        }
    }

    @Override
    public boolean replacePlayerMinions(MinionCommonData replacement, List<Integer> materialObjectIds) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                int inserted = jdbcClient.sql(INSERT_QUERY)
                    .param(replacement.getMasterObjectId())
                    .param(replacement.getObjectId())
                    .param(replacement.getMinionId())
                    .param(replacement.getName())
                    .param(replacement.getMinionGrade())
                    .param(replacement.getMinionLevel())
                    .param(replacement.getMinionGrowthPoint())
                    .update();
                if (inserted != 1) {
                    status.setRollbackOnly();
                    return false;
                }
                if (materialObjectIds != null) {
                    for (int materialObjectId : materialObjectIds) {
                        int deleted = jdbcClient.sql(DELETE_QUERY)
                            .param(replacement.getMasterObjectId())
                            .param(materialObjectId)
                            .update();
                        if (deleted != 1) {
                            status.setRollbackOnly();
                            return false;
                        }
                    }
                }
                return true;
            }));
        } catch (RuntimeException e) {
            log.error(I18n.get("log.2527877e3b8c", replacement.getMinionId(), replacement.getName()), e);
            return false;
        }
    }

    /**
     * 执行随从进化（更新 minion_id 与 level，重置成长点）。
     * Evolves a minion (updates minion_id and level, resets growth points).
     * @param player 玩家 / player
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public boolean evolutionMinion(Player player, MinionCommonData minionCommonData) {
        try {
            return jdbcClient.sql(EVOLUTION_QUERY)
                .param(minionCommonData.getMinionId())
                .param(minionCommonData.getMinionLevel())
                .param(minionCommonData.getMasterObjectId())
                .param(minionCommonData.getObjectId())
                .update() == 1;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.188874aa8e51", minionCommonData.getMinionId()), e);
            return false;
        }
    }

    /**
     * 锁定或解锁指定随从。
     * Locks or unlocks the specified minion.
     * @param player 玩家 / player
     * @param minionObjId 仆从对象 ID / minion object id
     * @param isLocked 锁定标记 / lock flag
     */
    @Override
    public boolean lockMinions(Player player, int minionObjId, int isLocked) {
        try {
            return jdbcClient.sql(LOCK_QUERY)
                .param(isLocked)
                .param(player.getObjectId())
                .param(minionObjId)
                .update() == 1;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.65bb4c6328f8", minionObjId), e);
            return false;
        }
    }

    /**
     * 保存随从的增益（doping）背包内容。
     * Saves the minion doping bag contents.
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

        try {
            StringBuilder itemIds = new StringBuilder();

            itemIds.append(bag.getFoodItem() > 0 ? bag.getFoodItem() : "0");
            itemIds.append(",");
            itemIds.append(bag.getDrinkItem() > 0 ? bag.getDrinkItem() : "0");

            for (int itemId : bag.getScrollsUsed()) {
                itemIds.append(",").append(itemId > 0 ? itemId : "0");
            }

            return jdbcClient.sql(UPDATE_DOPING_QUERY)
                .param(itemIds.toString())
                .param(player.getObjectId())
                .param(minionCommonData.getObjectId())
                .update() == 1;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.0c8cc68553ee", minionCommonData.getObjectId()), e);
            return false;
        }
    }

    @Override
    public int[] getUsedIDs() {
        try {
            List<Integer> ids = jdbcClient.sql("SELECT object_id FROM player_minions")
                .query((rs, _) -> rs.getInt(1))
                .list();
            return ids.stream().mapToInt(Integer::intValue).toArray();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.6b0198f98c6b", 0), e);
            return new int[0];
        }
    }

    /**
     * 从数据库读取并写回随从生日时间戳。
     * Loads the minion birthday timestamp from the database into the model.
     * @param minionCommonData 随从公共数据 / minion common data
     */
    @Override
    public void saveBirthday(MinionCommonData minionCommonData) {
        try {
            Timestamp birthday = jdbcClient.sql(SELECT_BIRTHDAY_QUERY)
                .param(minionCommonData.getMasterObjectId())
                .param(minionCommonData.getObjectId())
                .query(Timestamp.class)
                .optional()
                .orElse(null);

            if (birthday != null) {
                minionCommonData.setBirthday(birthday);
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8896cfb9d7f2", minionCommonData.getMasterObjectId()), e);
        }
    }

    /**
     * 清理所属玩家已不存在的孤儿随从记录。
     * Cleans orphaned minion rows whose owner player no longer exists.
     * @return 删除行数 / deleted row count
     */
    
    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
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
