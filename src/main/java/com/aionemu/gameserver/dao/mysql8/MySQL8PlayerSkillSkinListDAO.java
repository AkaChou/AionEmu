package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerSkillSkinListDAO;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;

/**
 * 玩家技能皮肤列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerSkillSkinListDAO.
 */
@Slf4j
public class MySQL8PlayerSkillSkinListDAO extends PlayerSkillSkinListDAO {

    /** 加载技能皮肤列表 SQL / Load skill skin list SQL */
    private static final String LOAD_QUERY = "SELECT `skin_id`, `remaining`, `active` FROM `player_skill_skins` WHERE `player_id`=?";
    /** 插入或更新技能皮肤 SQL / Insert or update skill skin SQL */
    private static final String INSERT_QUERY = "INSERT INTO `player_skill_skins`(`player_id`, `skin_id`, `remaining`, `active`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`), `active` = VALUES(`active`)";
    /** 删除技能皮肤 SQL / Delete skill skin SQL */
    private static final String DELETE_QUERY = "DELETE FROM `player_skill_skins` WHERE `player_id`=? AND `skin_id`=?";
    /** 更新技能皮肤激活状态 SQL / Update skill skin active state SQL */
    private static final String UPDATE_ACTIVE_QUERY = "UPDATE `player_skill_skins` SET `active` = ? WHERE `player_id`=? AND `skin_id`=?";

    /**
     * 加载玩家技能皮肤列表。
     * Loads the player's skill skin list.
     *
     * player id
     *
     * @param playerId @return 技能皮肤列表 / skill skin list
     */
    @Override
    public SkillSkinList loadSkillSkinList(final int playerId) {
        final SkillSkinList tl = new SkillSkinList();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOAD_QUERY)) {

            stmt.setInt(1, playerId);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("skin_id");
                    int remaining = rset.getInt("remaining");
                    int active = rset.getInt("active");
                    tl.addEntry(id, remaining, active);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.7cb3e1e1c2aa", playerId, e));
        }

        return tl;
    }

    /**
     * 保存玩家技能皮肤。
     * Stores a player skill skin.
     *
     * 玩家 / player
     * @param entry 技能皮肤条目 / skill skin entry
     * whether successful
     */
    @Override
    public boolean storeSkillSkins(Player player, SkillSkin entry) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, entry.getId());
            stmt.setInt(3, entry.getExpireTime());
            stmt.setInt(4, entry.getIsActive());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.b49a1a09f26e", player.getObjectId(), e));
            return false;
        }
    }

    /**
     * 激活指定技能皮肤。
     * Activates the specified skill skin.
     *
     * player object id
     * skin id
     * whether successful
     */
    @Override
    public boolean setActive(final int playerObjId, final int skinId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ACTIVE_QUERY)) {

            stmt.setInt(1, 1); // active = 1
            stmt.setInt(2, playerObjId);
            stmt.setInt(3, skinId);
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.1c834a3bbe53", playerObjId, skinId, e));
            return false;
        }
    }

    /**
     * 取消激活指定技能皮肤。
     * Deactivates the specified skill skin.
     *
     * player object id
     * skin id
     * whether successful
     */
    @Override
    public boolean setDeactive(final int playerObjId, final int skinId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ACTIVE_QUERY)) {

            stmt.setInt(1, 0); // active = 0
            stmt.setInt(2, playerObjId);
            stmt.setInt(3, skinId);
            int updated = stmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.467b5e662059", playerObjId, skinId, e));
            return false;
        }
    }

    /**
     * 移除指定技能皮肤。
     * Removes the specified skill skin.
     *
     * player id
     * skin id
     * whether successful
     */
    @Override
    public boolean removeSkillSkin(int playerId, int skinId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, skinId);
            int deleted = stmt.executeUpdate();
            return deleted > 0;
        } catch (SQLException e) {
            log.error(I18n.get("log.84df7864194f", playerId, e));
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
