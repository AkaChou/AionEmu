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
	 * @return 类名 / class name
	 */
	@Override
	public String getClassName() {
		return FriendListDAO.class.getName();
	}

	/**
	 * 加载玩家的好友列表。
	 * Loads the friend list for a player.
	 *
	 * @param player 玩家 / player
	 * @return 好友列表 / friend list
	 */
	public abstract FriendList load(final Player player);

	/**
	 * 将两名玩家添加为好友。
	 * Adds two players as friends.
	 *
	 * @param player 玩家 / player
	 * @param friend 好友 / friend
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean addFriends(final Player player, final Player friend);

	/**
	 * 删除好友关系。
	 * Deletes a friendship between two players.
	 *
	 * @param playerOid 玩家对象 ID / player object ID
	 * @param friendOid 好友对象 ID / friend object ID
	 * @return 是否成功 / whether successful
	 */
	public abstract boolean delFriends(final int playerOid, final int friendOid);

	/**
	 * 设置好友备注。
	 * Sets a note for a friend.
	 *
	 * @param playerId 玩家 ID / player ID
	 * @param friendId 好友 ID / friend ID
	 * @param notice 备注 / note
	 */
	public abstract void setFriendNote(final int playerId, final int friendId, final String notice);
}
