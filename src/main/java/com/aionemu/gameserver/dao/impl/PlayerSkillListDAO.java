package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 玩家技能列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerSkillListDAO.
 *
 * @author SoulKeeper
 * @author IceReaper, orfeo087, Avol, AEJTester
 * Updated for MySQL 8 support
 */
@Slf4j
public class PlayerSkillListDAO extends com.aionemu.gameserver.dao.PlayerSkillListDAO {

    /** 插入或更新技能等级 / Insert or update skill level */
    public static final String INSERT_QUERY = "INSERT INTO `player_skills` (`player_id`, `skill_id`, `skill_level`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `skill_level` = VALUES(`skill_level`)";

    /** 更新技能等级 / Update skill level */
    public static final String UPDATE_QUERY = "UPDATE `player_skills` SET skill_level = ? WHERE player_id = ? AND skill_id = ?";

    /** 更新技能皮肤信息 / Update skill skin info */
    public static final String UPDATE_SKIN_QUERY = "UPDATE `player_skills` SET skin_id = ?, skin_active_date = ?, " + "skin_expire_time = ?, skin_activated = ? WHERE player_id = ? AND skill_id = ?";

    /** 删除技能 / Delete a skill */
    public static final String DELETE_QUERY = "DELETE FROM `player_skills` WHERE `player_id` = ? AND skill_id = ?";

    /** 按玩家加载技能列表 / Select skills by player */
    public static final String SELECT_QUERY = "SELECT `skill_id`, `skill_level`, `skin_id`, `skin_active_date`, " + "`skin_expire_time`, `skin_activated` FROM `player_skills` WHERE `player_id` = ?";

    /** 待插入技能谓词（NEW 状态） / Predicate for skills to insert (NEW state) */
    private static final Predicate<PlayerSkillEntry> skillsToInsertPredicate = new Predicate<PlayerSkillEntry>() {
        @Override
        public boolean apply(PlayerSkillEntry input) {
            return input != null && PersistentState.NEW == input.getPersistentState();
        }
    };

    /** 待更新技能谓词（UPDATE_REQUIRED 状态） / Predicate for skills to update (UPDATE_REQUIRED state) */
    private static final Predicate<PlayerSkillEntry> skillsToUpdatePredicate = new Predicate<PlayerSkillEntry>() {
        @Override
        public boolean apply(PlayerSkillEntry input) {
            return input != null && PersistentState.UPDATE_REQUIRED == input.getPersistentState();
        }
    };

    /** 待删除技能谓词（DELETED 状态） / Predicate for skills to delete (DELETED state) */
    private static final Predicate<PlayerSkillEntry> skillsToDeletePredicate = new Predicate<PlayerSkillEntry>() {
        @Override
        public boolean apply(PlayerSkillEntry input) {
            return input != null && PersistentState.DELETED == input.getPersistentState();
        }
    };

    /**
     * 加载玩家技能列表。
     * Loads the player's skill list.
     *
     * player id
     * skill list
     */
    @Override
    public PlayerSkillList loadSkillList(int playerId) {
        List<PlayerSkillEntry> skills = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, playerId);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("skill_id");
                    int lv = rset.getInt("skill_level");
                    int skin_id = rset.getInt("skin_id");
                    Timestamp active_date = rset.getTimestamp("skin_active_date");
                    int expireTime = rset.getInt("skin_expire_time");
                    boolean isActivated = rset.getBoolean("skin_activated");

                    skills.add(new PlayerSkillEntry(id, false, false, lv, skin_id, active_date, expireTime, isActivated, PersistentState.UPDATED));
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.d0d2b39236dc", playerId, e));
        }

        return new PlayerSkillList(skills);
    }

    /**
     * 持久化玩家当前技能与已删除技能。
     * Persists the player's active and deleted skills.
     *
     * 玩家 / player
     * always true
     */
    @Override
    public boolean storeSkills(Player player) {
        List<PlayerSkillEntry> skillsActive = Lists.newArrayList(player.getSkillList().getAllSkills());
        skillsActive.addAll(Lists.newArrayList(player.getSkillList().getDeletedSkills()));
        return store(player, skillsActive);
    }

    /**
     * 在事务中删除、插入、更新技能及皮肤。
     * Deletes, inserts, and updates skills and skins within a transaction.
     *
     * @param player 玩家 / player
     * @param skills 技能条目列表 / list of skill entries
     */
    private boolean store(Player player, List<PlayerSkillEntry> skills) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                deleteSkills(con, player, skills);
                addSkills(con, player, skills);
                updateSkills(con, player, skills);
                updateSkinSkills(con, player, skills);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.6790b402b724", player.getObjectId(), e));
            return false;
        }

        for (PlayerSkillEntry skill : skills) {
            skill.setPersistentState(PersistentState.UPDATED);
        }
        return true;
    }

    /**
     * 批量插入 NEW 状态技能。
     * Batch-inserts skills in NEW state.
     *
     * connection
     * 玩家 / player
     * @param skills 技能条目列表 / list of skill entries
     * SQL exception
     */
    private void addSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {

        Collection<PlayerSkillEntry> skillsToInsert = Collections2.filter(skills, skillsToInsertPredicate);

        if (GenericValidator.isBlankOrNull(skillsToInsert)) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(INSERT_QUERY)) {
            for (PlayerSkillEntry skill : skillsToInsert) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getSkillId());
                ps.setInt(3, skill.getSkillLevel());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /**
     * 批量更新 UPDATE_REQUIRED 状态技能等级。
     * Batch-updates skill levels in UPDATE_REQUIRED state.
     *
     * connection
     * 玩家 / player
     * @param skills 技能条目列表 / list of skill entries
     * SQL exception
     */
    private void updateSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {

        Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);

        if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(UPDATE_QUERY)) {
            for (PlayerSkillEntry skill : skillsToUpdate) {
                ps.setInt(1, skill.getSkillLevel());
                ps.setInt(2, player.getObjectId());
                ps.setInt(3, skill.getSkillId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /**
     * 批量更新 UPDATE_REQUIRED 状态技能皮肤信息。
     * Batch-updates skill skin info in UPDATE_REQUIRED state.
     *
     * connection
     * 玩家 / player
     * @param skills 技能条目列表 / list of skill entries
     * SQL exception
     */
    private void updateSkinSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {

        Collection<PlayerSkillEntry> skillsToUpdate = Collections2.filter(skills, skillsToUpdatePredicate);

        if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(UPDATE_SKIN_QUERY)) {
            for (PlayerSkillEntry skill : skillsToUpdate) {
                ps.setInt(1, skill.getSkinId());
                ps.setTimestamp(2, skill.getSkinActiveTime());
                ps.setInt(3, skill.getSkinExpireTime());
                ps.setBoolean(4, skill.isActivated());
                ps.setInt(5, player.getObjectId());
                ps.setInt(6, skill.getSkillId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /**
     * 批量删除 DELETED 状态技能。
     * Batch-deletes skills in DELETED state.
     *
     * connection
     * 玩家 / player
     * @param skills 技能条目列表 / list of skill entries
     * SQL exception
     */
    private void deleteSkills(Connection con, Player player, List<PlayerSkillEntry> skills) throws SQLException {

        Collection<PlayerSkillEntry> skillsToDelete = Collections2.filter(skills, skillsToDeletePredicate);

        if (GenericValidator.isBlankOrNull(skillsToDelete)) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {
            for (PlayerSkillEntry skill : skillsToDelete) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getSkillId());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /**
     * 获取技能皮肤激活日期。
     * Returns the skill skin active date.
     *
     * player object id
     * skill id
     * @return 激活时间戳；无记录返回 null / active timestamp, or null if none
     */
    @Override
    public Timestamp getSkinSkillActiveDateById(int playerObjId, int skillId) {
        String query = "SELECT `skin_active_date` FROM `player_skills` " + "WHERE `player_id` = ? AND `skill_id` = ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {

            s.setInt(1, playerObjId);
            s.setInt(2, skillId);

            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("skin_active_date");
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.32a835694574", playerObjId, skillId, e));
        }

        return null;
    }

    /**
     * 获取技能皮肤过期时间。
     * Returns the skill skin expire time.
     *
     * player object id
     * skill id
     * @return 过期时间；无记录返回 0 / expire time, or 0 if none
     */
    @Override
    public int getSkinExpireTime(int playerObjId, int skillId) {
        String query = "SELECT `skin_expire_time` FROM `player_skills` " + "WHERE `player_id` = ? AND `skill_id` = ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {

            s.setInt(1, playerObjId);
            s.setInt(2, skillId);

            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("skin_expire_time");
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.c08c665f848c", playerObjId, skillId, e));
        }

        return 0;
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
}
