package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.FriendList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.FriendPo;
import com.mybatisflex.core.service.IService;

/**
 * 好友列表数据访问层。
 * Friend list data access layer.
 */
public interface FriendDao extends IService<FriendPo> {

	/**
	 * 加载玩家的好友列表。
	 * Loads the friend list for a player.
	 * @param player 玩家 / player
	 * @return 好友列表 / friend list
	 */
	FriendList load(Player player);

	/**
	 * 将两名玩家添加为好友。
	 * Adds two players as friends.
	 * @param player 玩家 / player
	 * @param friend 好友 / friend
	 */
	void addFriends(Player player, Player friend);

	/**
	 * 删除好友关系。
	 * Deletes a friendship between two players.
	 * @param playerOid 玩家对象 ID / player object ID
	 * @param friendOid 好友对象 ID / friend object ID
	 * @return 是否成功 / whether successful
	 */
	boolean delFriends(int playerOid, int friendOid);

	/**
	 * 设置好友备注。
	 * Sets a note for a friend.
	 * @param playerId 玩家 ID / player ID
	 * @param friendId 好友 ID / friend ID
	 * @param notice 备注 / note
	 */
	void setFriendNote(int playerId, int friendId, String notice);
}
