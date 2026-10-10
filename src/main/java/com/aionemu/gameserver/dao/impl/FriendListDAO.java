package com.aionemu.gameserver.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;

import lombok.extern.slf4j.Slf4j;

/**
 * 好友列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of FriendListDAO using JdbcClient.
 * @author Ben
 */
@Slf4j
public class FriendListDAO extends com.aionemu.gameserver.dao.FriendListDAO {

    /** 加载好友列表 SQL / Load friend list SQL*/
    private static final String LOAD_QUERY = "SELECT * FROM `friends` WHERE `player`=?";
    /** 添加好友 SQL / Add friend SQL*/
    private static final String ADD_QUERY = "INSERT INTO `friends` (`player`,`friend`) VALUES (?, ?)";
    /** 删除好友 SQL / Delete friend SQL*/
    private static final String DEL_QUERY = "DELETE FROM friends WHERE player = ? AND friend = ?";
    /** 设置好友备注 SQL / Set friend note SQL */
    private static final String SET_NOTE = "UPDATE `friends` SET `note` = ? WHERE `player` = ? AND `friend` = ?";

    private final JdbcClient jdbcClient;

    public FriendListDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public FriendListDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家好友列表。
     * Loads the player's friend list.
     * @param player 玩家 / player
     * @return 好友列表 / friend list
     */
    @Override
    public FriendList load(final Player player) {
        final List<Friend> friends = new ArrayList<>();

        try {
            PlayerDAO dao = DAOManager.getDAO(PlayerDAO.class);
            jdbcClient.sql(LOAD_QUERY)
                .param(player.getObjectId())
                .query((rs, rowNum) -> {
                    int objId = rs.getInt("friend");
                    PlayerCommonData pcd = dao.loadPlayerCommonData(objId);
                    if (pcd != null) {
                        friends.add(new Friend(pcd));
                    }
                    return null;
                })
                .list();
        } catch (Exception e) {
            log.error(I18n.get("log.76f2514feafb", player.getObjectId(), " from DB"), e);
        }
        return new FriendList(player, friends);
    }

    /**
     * 双向添加好友关系。
     * Adds a mutual friendship between two players.
     * @param player 玩家 / player
     * @param friend 好友 / friend
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean addFriends(final Player player, final Player friend) {
        try {
            jdbcClient.sql(ADD_QUERY)
                .param(player.getObjectId())
                .param(friend.getObjectId())
                .update();
            jdbcClient.sql(ADD_QUERY)
                .param(friend.getObjectId())
                .param(player.getObjectId())
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.3eacec776c80", player.getObjectId(), friend.getObjectId()), e);
            return false;
        }
    }

    /**
     * 双向删除好友关系。
     * Deletes a mutual friendship between two players.
     * @param playerOid 玩家对象 ID / player object id
     * @param friendOid 好友对象 ID / friend object id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean delFriends(final int playerOid, final int friendOid) {
        try {
            jdbcClient.sql(DEL_QUERY)
                .param(playerOid)
                .param(friendOid)
                .update();
            jdbcClient.sql(DEL_QUERY)
                .param(friendOid)
                .param(playerOid)
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.11aae36500f7", playerOid, friendOid), e);
            return false;
        }
    }

    /**
     * 设置好友备注。
     * Sets a note for a friend.
     * @param playerId 玩家 ID / player id
     * @param friendId 好友 ID / friend id
     * @param note 备注 / note
     */
    @Override
    public void setFriendNote(final int playerId, final int friendId, final String note) {
        try {
            jdbcClient.sql(SET_NOTE)
                .param(note)
                .param(playerId)
                .param(friendId)
                .update();
        } catch (Exception e) {
            log.error(I18n.get("log.7761eea711e0", playerId, friendId), e);
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     * @param database 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
