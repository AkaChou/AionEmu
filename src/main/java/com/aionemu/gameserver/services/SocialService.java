package com.aionemu.gameserver.services;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.BlockedPlayer;
import com.aionemu.gameserver.model.gameobjects.player.Friend;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BLOCK_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_BLOCK_RESPONSE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_NOTIFY;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FRIEND_RESPONSE;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.world.World;

/**
 * 社交服务，处理好友与黑名单的增删改。
 * Social service that handles friend and block-list add/remove/update.
 */
public class SocialService {

	/**
	 * 将目标加入玩家黑名单。
	 * Adds the target to the player's block list.
	 *
	 * operator
	 * @param blockedPlayer 被屏蔽玩家 / blocked player
	 * block reason
	 *
	 * @return 添加成功时为 {@code true} / {@code true} if added
	 */
	public static boolean addBlockedUser(Player player, Player blockedPlayer, String reason) {
		if (DAOManager.getDAO(BlockListDAO.class).addBlockedUser(player.getObjectId(), blockedPlayer.getObjectId(),
				reason)) {
			player.getBlockList().add(new BlockedPlayer(blockedPlayer.getCommonData(), reason));
			player.getClientConnection()
					.sendPacket(new SM_BLOCK_RESPONSE(SM_BLOCK_RESPONSE.BLOCK_SUCCESSFUL, blockedPlayer.getName()));
			player.getClientConnection().sendPacket(new SM_BLOCK_LIST());
			return true;
		}
		return false;
	}

	/**
	 * 从玩家黑名单中移除指定用户。
	 * Removes the given user from the player's block list.
	 *
	 * operator
	 *
	 * @param blockedUserId 被屏蔽玩家 objectId / blocked player object id
	 * @param blockedUserId 被屏蔽玩家 ID / Blocked player ID
	 * @return 移除成功时为 {@code true} / {@code true} if removed
	 */
	public static boolean deleteBlockedUser(Player player, int blockedUserId) {
		if (DAOManager.getDAO(BlockListDAO.class).delBlockedUser(player.getObjectId(), blockedUserId)) {
			player.getBlockList().remove(blockedUserId);
			player.getClientConnection().sendPacket(new SM_BLOCK_RESPONSE(SM_BLOCK_RESPONSE.UNBLOCK_SUCCESSFUL,
					DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(blockedUserId).getName()));
			player.getClientConnection().sendPacket(new SM_BLOCK_LIST());
			return true;
		}
		return false;
	}

	/**
	 * 更新黑名单条目的屏蔽原因。
	 * Updates the block reason for a blocked player entry.
	 *
	 * operator
	 * @param target 黑名单条目 / blocked player entry
	 * new reason
	 *
	 * @return 有变更且持久化成功返回 true / true if changed and persisted
	 */
	public static boolean setBlockedReason(Player player, BlockedPlayer target, String reason) {
		if (!target.getReason().equals(reason)) {
			if (DAOManager.getDAO(BlockListDAO.class).setReason(player.getObjectId(), target.getObjId(), reason)) {
				target.setReason(reason);
				player.getClientConnection().sendPacket(new SM_BLOCK_LIST());
				return true;
			}
		}
		return false;
	}

	/**
	 * 建立双向好友关系并同步双方好友列表。
	 * Creates a mutual friendship and syncs both friend lists.
	 *
	 * first player
	 * second player
	 */
	public static void makeFriends(Player friend1, Player friend2) {
		DAOManager.getDAO(FriendListDAO.class).addFriends(friend1, friend2);
		friend1.getFriendList().addFriend(new Friend(friend2.getCommonData()));
		friend2.getFriendList().addFriend(new Friend(friend1.getCommonData()));
		friend1.getClientConnection().sendPacket(new SM_FRIEND_LIST());
		friend2.getClientConnection().sendPacket(new SM_FRIEND_LIST());
		friend1.getClientConnection()
				.sendPacket(new SM_FRIEND_RESPONSE(friend2.getName(), SM_FRIEND_RESPONSE.TARGET_ADDED));
		friend2.getClientConnection()
				.sendPacket(new SM_FRIEND_RESPONSE(friend1.getName(), SM_FRIEND_RESPONSE.TARGET_ADDED));
	}

	/**
	 * 删除好友关系；对方在线时同步通知。
	 * Deletes a friendship; notifies the other side if online.
	 *
	 * @param deleter 发起删除的玩家 / player who deletes
	 * @param exFriend2Id 被删除好友 objectId / former friend object id
	 */
	public static void deleteFriend(Player deleter, int exFriend2Id) {
		if (DAOManager.getDAO(FriendListDAO.class).delFriends(deleter.getObjectId(), exFriend2Id)) {
			Player friend2Player = PlayerService.getCachedPlayer(exFriend2Id);
			if (friend2Player == null) {
				friend2Player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(exFriend2Id);
			}
			String friend2Name = friend2Player != null ? friend2Player.getName()
					: DAOManager.getDAO(PlayerDAO.class).loadPlayerCommonData(exFriend2Id).getName();
			deleter.getFriendList().delFriend(exFriend2Id);
			deleter.getClientConnection().sendPacket(new SM_FRIEND_LIST());
			deleter.getClientConnection()
					.sendPacket(new SM_FRIEND_RESPONSE(friend2Name, SM_FRIEND_RESPONSE.TARGET_REMOVED));
			if (friend2Player != null) {
				friend2Player.getFriendList().delFriend(deleter.getObjectId());
				if (friend2Player.isOnline()) {
					friend2Player.getClientConnection()
							.sendPacket(new SM_FRIEND_NOTIFY(SM_FRIEND_NOTIFY.DELETED, deleter.getName()));
					friend2Player.getClientConnection().sendPacket(new SM_FRIEND_LIST());
				}
			}
		}
	}

	/**
	 * 设置好友备注并刷新好友列表包。
	 * Sets a friend note and refreshes the friend list packet.
	 *
	 * operator
	 * friend entry
	 * note
	 */
	public static void setFriendNote(Player player, Friend friend, String notice) {
		friend.setNote(notice);
		DAOManager.getDAO(FriendListDAO.class).setFriendNote(player.getObjectId(), friend.getOid(), notice);
		player.getClientConnection().sendPacket(new SM_FRIEND_LIST());
	}
}
