package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 好友列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of FriendListDAO.
 *
 * @author Ben
 */
@Slf4j
public class MySQL8FriendListDAO extends FriendListDAO {

    /** 加载好友列表 SQL / Load friend list SQL*/
    private static final String LOAD_QUERY = "SELECT * FROM `friends` WHERE `player`=?";
    /** 添加好友 SQL / Add friend SQL*/
    private static final String ADD_QUERY = "INSERT INTO `friends` (`player`,`friend`) VALUES (?, ?)";
    /** 删除好友 SQL / Delete friend SQL*/
    private static final String DEL_QUERY = "DELETE FROM friends WHERE player = ? AND friend = ?";
    /** 设置好友备注 SQL / Set friend note SQL */
    private static final String SET_NOTE = "UPDATE `friends` SET `note` = ? WHERE `player` = ? AND `friend` = ?";

    /**
     * 加载玩家好友列表。
     * Loads the player's friend list.
     *
     * 玩家 / player
     * friend list
     */
    @Override
    public FriendList load(final Player player) {
        final List<Friend> friends = new ArrayList<Friend>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOAD_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                PlayerDAO dao = DAOManager.getDAO(PlayerDAO.class);
                while (rset.next()) {
                    int objId = rset.getInt("friend");
                    PlayerCommonData pcd = dao.loadPlayerCommonData(objId);
                    if (pcd != null) {
                        Friend friend = new Friend(pcd);
                        friends.add(friend);
                    }
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.76f2514feafb", player.getObjectId(), " from DB", e));
        }
        return new FriendList(player, friends);
    }

    /**
     * 双向添加好友关系。
     * Adds a mutual friendship between two players.
     *
     * 玩家 / player
     * friend
     * whether successful
     */
    @Override
    public boolean addFriends(final Player player, final Player friend) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(ADD_QUERY)) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, friend.getObjectId());
                ps.addBatch();
                ps.setInt(1, friend.getObjectId());
                ps.setInt(2, player.getObjectId());
                ps.addBatch();
                ps.executeBatch();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.3eacec776c80", player.getObjectId(), friend.getObjectId(), e));
            return false;
        }
    }

    /**
     * 双向删除好友关系。
     * Deletes a mutual friendship between two players.
     *
     * player object id
     * friend object id
     * whether successful
     */
    @Override
    public boolean delFriends(final int playerOid, final int friendOid) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(DEL_QUERY)) {
                ps.setInt(1, playerOid);
                ps.setInt(2, friendOid);
                ps.addBatch();
                ps.setInt(1, friendOid);
                ps.setInt(2, playerOid);
                ps.addBatch();
                ps.executeBatch();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.11aae36500f7", playerOid, friendOid, e));
            return false;
        }
    }

    /**
     * 设置好友备注。
     * Sets a note for a friend.
     *
     * player id
     * friend id
     * note
     */
    @Override
    public void setFriendNote(final int playerId, final int friendId, final String note) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SET_NOTE)) {

            stmt.setString(1, note);
            stmt.setInt(2, playerId);
            stmt.setInt(3, friendId);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error(I18n.get("log.7761eea711e0", playerId, friendId, e));
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * @param s 数据库名 / database name
     * @param i 主版本 / major version
     * @param i1 次版本 / minor version
     * whether supported
     */
    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL8DAOUtils.supports(s, i, i1);
    }
}
