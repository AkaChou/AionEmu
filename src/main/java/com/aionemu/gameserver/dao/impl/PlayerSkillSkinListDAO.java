package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家技能皮肤列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerSkillSkinListDAO using JdbcClient.
 */
@Slf4j
@Repository
public class PlayerSkillSkinListDAO extends com.aionemu.gameserver.dao.PlayerSkillSkinListDAO {

    /** 加载技能皮肤列表 SQL / Load skill skin list SQL */
    private static final String LOAD_QUERY = "SELECT `skin_id`, `remaining`, `active` FROM `player_skill_skins` WHERE `player_id`=?";
    /** 插入或更新技能皮肤 SQL / Insert or update skill skin SQL */
    private static final String INSERT_QUERY = "INSERT INTO `player_skill_skins`(`player_id`, `skin_id`, `remaining`, `active`) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`), `active` = VALUES(`active`)";
    /** 删除技能皮肤 SQL / Delete skill skin SQL */
    private static final String DELETE_QUERY = "DELETE FROM `player_skill_skins` WHERE `player_id`=? AND `skin_id`=?";
    /** 更新技能皮肤激活状态 SQL / Update skill skin active state SQL */
    private static final String UPDATE_ACTIVE_QUERY = "UPDATE `player_skill_skins` SET `active` = ? WHERE `player_id`=? AND `skin_id`=?";

    private final JdbcClient jdbcClient;

    public PlayerSkillSkinListDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerSkillSkinListDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家技能皮肤列表。
     * Loads the player's skill skin list.
     * @param playerId 玩家 ID / player id
     * @return 技能皮肤列表 / skill skin list
     */
    @Override
    public SkillSkinList loadSkillSkinList(final int playerId) {
        final SkillSkinList tl = new SkillSkinList();

        try {
            jdbcClient.sql(LOAD_QUERY)
                .param(playerId)
                .query((rset, rowNum) -> {
                    int id = rset.getInt("skin_id");
                    int remaining = rset.getInt("remaining");
                    int active = rset.getInt("active");
                    tl.addEntry(id, remaining, active);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7cb3e1e1c2aa", playerId), e);
        }

        return tl;
    }

    /**
     * 保存玩家技能皮肤。
     * Stores a player skill skin.
     * @param player 玩家 / player
     * @param entry 技能皮肤条目 / skill skin entry
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean storeSkillSkins(Player player, SkillSkin entry) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(player.getObjectId())
                .param(entry.getId())
                .param(entry.getExpireTime())
                .param(entry.getIsActive())
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.b49a1a09f26e", player.getObjectId()), e);
            return false;
        }
    }

    /**
     * 激活指定技能皮肤。
     * Activates the specified skill skin.
     * @param playerObjId 玩家对象 ID / player object id
     * @param skinId 外观 ID / skin id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean setActive(final int playerObjId, final int skinId) {
        try {
            return jdbcClient.sql(UPDATE_ACTIVE_QUERY)
                .param(1)
                .param(playerObjId)
                .param(skinId)
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.1c834a3bbe53", playerObjId, skinId), e);
            return false;
        }
    }

    /**
     * 取消激活指定技能皮肤。
     * Deactivates the specified skill skin.
     * @param playerObjId 玩家对象 ID / player object id
     * @param skinId 外观 ID / skin id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean setDeactive(final int playerObjId, final int skinId) {
        try {
            return jdbcClient.sql(UPDATE_ACTIVE_QUERY)
                .param(0)
                .param(playerObjId)
                .param(skinId)
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.467b5e662059", playerObjId, skinId), e);
            return false;
        }
    }

    /**
     * 移除指定技能皮肤。
     * Removes the specified skill skin.
     * @param playerId 玩家 ID / player id
     * @param skinId 外观 ID / skin id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean removeSkillSkin(int playerId, int skinId) {
        try {
            return jdbcClient.sql(DELETE_QUERY)
                .param(playerId)
                .param(skinId)
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.84df7864194f", playerId), e);
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
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
