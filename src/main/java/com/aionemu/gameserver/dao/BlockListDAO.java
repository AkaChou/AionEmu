package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 负责保存与加载玩家黑名单数据。
 * Responsible for saving and loading data on players' block lists.
 *
 * @author Ben
 */
public abstract class BlockListDAO implements DAO {

	/**
	 * 加载指定玩家的黑名单。
	 * Loads the blocklist for the given player.
	 *
	 * 玩家 / player
	 * block list
	 */
	public abstract BlockList load(Player player);

	/**
	 * 将目标对象 ID 加入指定玩家的黑名单。
	 * Adds the given object ID to the list of blocked players for the given player.
	 *
	 * @param playerObjId 被编辑黑名单的玩家 ID / ID of player whose blocklist is edited
	 * @param objIdToBlock 要加入黑名单的玩家 ID / ID of player to add to the blocklist
	 * block reason
	 * whether successful
	 */
	public abstract boolean addBlockedUser(int playerObjId, int objIdToBlock, String reason);

	/**
	 * 从指定玩家的黑名单中删除目标对象 ID。
	 * Deletes the given object ID from the list of blocked players for the given player.
	 *
	 * @param playerObjId 被编辑黑名单的玩家 ID / ID of player whose blocklist is edited
	 * @param objIdToDelete 要从黑名单移除的玩家 ID / ID of player to remove from the blocklist
	 * whether successful
	 */
	public abstract boolean delBlockedUser(int playerObjId, int objIdToDelete);

	/**
	 * 设置屏蔽某玩家的原因。
	 * Sets the reason for blocking a player.
	 *
	 * @param playerObjId 被编辑黑名单的玩家对象 ID / object ID of the player whose list is being edited
	 * @param blockedObjId 被屏蔽玩家的对象 ID / object ID of the player whose reason is being edited
	 * @param reason 要设置的原因 / the reason to be set
	 * whether successful
	 */
	public abstract boolean setReason(int playerObjId, int blockedObjId, String reason);

	/**
	 * 返回本 DAO 的唯一类名标识。
	 * Returns the unique class-name identifier for this DAO.
	 *
	 * class name
	 */
	@Override
	public String getClassName() {
		return BlockListDAO.class.getName();
	}
}
