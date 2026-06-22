package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ben
 */
public class MySQL8FriendListDAO extends FriendListDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8FriendListDAO.class);
    
    private static final String LOAD_QUERY = "SELECT * FROM `friends` WHERE `player`=?";
    private static final String ADD_QUERY = "INSERT INTO `friends` (`player`,`friend`) VALUES (?, ?)";
    private static final String DEL_QUERY = "DELETE FROM friends WHERE player = ? AND friend = ?";
    private static final String SET_NOTE = "UPDATE `friends` SET `note` = ? WHERE `player` = ? AND `friend` = ?";

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
            log.error("Could not restore FriendList data for player: " + player.getObjectId() + " from DB", e);
        }
        return new FriendList(player, friends);
    }

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
            log.error("Error adding friends: {} and {}", player.getObjectId(), friend.getObjectId(), e);
            return false;
        }
    }

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
            log.error("Error deleting friends: {} and {}", playerOid, friendOid, e);
            return false;
        }
    }

    @Override
    public void setFriendNote(final int playerId, final int friendId, final String note) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SET_NOTE)) {
            
            stmt.setString(1, note);
            stmt.setInt(2, playerId);
            stmt.setInt(3, friendId);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error setting friend note for player: {} friend: {}", playerId, friendId, e);
        }
    }

    @Override
    public boolean supports(String s, int i, int i1) {
        return MySQL8DAOUtils.supports(s, i, i1);
    }
}