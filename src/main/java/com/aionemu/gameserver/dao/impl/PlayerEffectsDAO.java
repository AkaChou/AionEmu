package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 玩家技能效果（Buff）持久化 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerEffectsDAO.
 *
 * @author ATracer, Updated for MySQL 8
 */
@Slf4j
public class PlayerEffectsDAO extends com.aionemu.gameserver.dao.PlayerEffectsDAO {


    /** 插入或更新玩家效果 / Insert or update a player effect */
    private static final String INSERT_QUERY = "INSERT INTO `player_effects` (`player_id`, `skill_id`, `skill_lvl`, `current_time`, `end_time`) " + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + "`skill_lvl` = VALUES(`skill_lvl`), `current_time` = VALUES(`current_time`), `end_time` = VALUES(`end_time`)";
    /** 删除玩家全部效果 / Delete all effects of a player */
    private static final String DELETE_QUERY = "DELETE FROM `player_effects` WHERE `player_id` = ?";
    /** 查询玩家全部效果 / Select all effects for a player */
    private static final String SELECT_QUERY = "SELECT `skill_id`, `skill_lvl`, `current_time`, `end_time` FROM `player_effects` WHERE `player_id` = ?";
    /** 删除已过期效果 / Delete expired effects */
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_effects` WHERE `end_time` < ?";

    /** 可持久化效果过滤条件（剩余时间大于 28 秒） / Predicate for insertable effects (remaining time > 28s) */
    private static final Predicate<Effect> INSERTABLE_EFFECTS_PREDICATE = effect -> effect != null
        && !effect.getSkillTemplate().isNoSaveOnLogout() && effect.getRemainingTime() > 28000;

    /**
     * 加载玩家技能效果到效果控制器。
     * Loads the player's skill effects into the effect controller.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadPlayerEffects(Player player) {
        List<SavedEffect> savedEffects = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int skillId = rset.getInt("skill_id");
                    int skillLvl = rset.getInt("skill_lvl");
                    int remainingTime = rset.getInt("current_time");
                    long endTime = rset.getLong("end_time");

                    if (remainingTime > 0) {
                        savedEffects.add(new SavedEffect(skillId, skillLvl, remainingTime, endTime));
                    }
                }
            }

            for (SavedEffect effect : savedEffects) {
                player.getEffectController().addSavedEffect(
                    effect.skillId,
                    effect.skillLvl,
                    effect.remainingTime,
                    effect.endTime
                );
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.8532fdc00af7", player.getObjectId(), e), e);
        }

        player.getEffectController().broadCastEffects();
    }

    /**
     * 持久化玩家当前可保存的技能效果。
     * Persists the player's currently insertable skill effects.
     *
     * @param player 玩家 / player
     */
    @Override
    public void storePlayerEffects(Player player) {
        List<Effect> validEffects = new ArrayList<>();

        for (Effect effect : player.getEffectController().getAbnormalEffectsToShow()) {
            if (INSERTABLE_EFFECTS_PREDICATE.test(effect)) {
                validEffects.add(effect);
            }
        }

        if (validEffects.isEmpty()) {
            deletePlayerEffects(player);
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_QUERY)) {
                deleteStmt.setInt(1, player.getObjectId());
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;

                for (Effect effect : validEffects) {
                    insertStmt.setInt(1, player.getObjectId());
                    insertStmt.setInt(2, effect.getSkillId());
                    insertStmt.setInt(3, effect.getSkillLevel());
                    insertStmt.setInt(4, effect.getRemainingTime());
                    insertStmt.setLong(5, persistedEndTime(effect));
                    insertStmt.addBatch();
                    batchCount++;

                    if (batchCount % 50 == 0) {
                        insertStmt.executeBatch();
                    }
                }

                if (batchCount % 50 != 0) {
                    insertStmt.executeBatch();
                }
            }

            con.commit();

        } catch (SQLException e) {
            log.error(I18n.get("log.db9bf45e33ff", player.getObjectId(), e), e);
        }
    }

    private static long persistedEndTime(Effect effect) {
        var template = effect.getSkillTemplate();
        return template.isSpendTimeOnLogout() || CustomConfig.ABYSSXFORM_LOGOUT && template.isDeityAvatar()
            ? effect.getEndTime() : Long.MAX_VALUE;
    }

    /**
     * 删除玩家全部效果记录。
     * Deletes all effect records of the player.
     *
     * @param player 玩家 / player
     */
    private void deletePlayerEffects(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.5703018fc477", player.getObjectId(), e), e);
        }
    }

    /**
     * 批量清理已过期的效果记录。
     * Batch-deletes expired effect records.
     */
    public void deleteExpiredEffects() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {

            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                log.info(I18n.get("log.4854575cda2a", deleted));
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.217d09d4548d", e), e);
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    /**
     * 从数据库加载的效果快照。
     * Snapshot of an effect loaded from the database.
     */
    private static class SavedEffect {
        final int skillId;
        final int skillLvl;
        final int remainingTime;
        final long endTime;

        SavedEffect(int skillId, int skillLvl, int remainingTime, long endTime) {
            this.skillId = skillId;
            this.skillLvl = skillLvl;
            this.remainingTime = remainingTime;
            this.endTime = endTime;
        }
    }
}
