package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 好友列表数据访问对象。
 * Friend list data access object.
 */
public abstract class FriendListDAO implements DAO {
	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public String getClassName() {
		return FriendListDAO.class.getName();
	}

	/**
	 * 加载玩家的好友列表。
	 * Loads the friend list for a player.
	 *
	 * 玩家 / player
	 * friend list
	 */
	public abstract FriendList load(final Player player);

	/**
	 * 将两名玩家添加为好友。
	 * Adds two players as friends.
	 *
	 * 玩家 / player
	 * friend
	 * whether successful
	 */
	public abstract boolean addFriends(final Player player, final Player friend);

	/**
	 * 删除好友关系。
	 * Deletes a friendship between two players.
	 *
	 * player object ID
	 * friend object ID
	 * whether successful
	 */
	public abstract boolean delFriends(final int playerOid, final int friendOid);

	/**
	 * 设置好友备注。
	 * Sets a note for a friend.
	 *
	 * player ID
	 * friend ID
	 * note
	 */
	public abstract void setFriendNote(final int playerId, final int friendId, final String notice);
}
