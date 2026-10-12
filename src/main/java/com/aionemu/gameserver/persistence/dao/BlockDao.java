package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.BlockList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.BlockPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家黑名单数据访问层。
 * Player block list data access layer.
 */
public interface BlockDao extends IService<BlockPo> {

	/**
	 * 加载指定玩家的黑名单。
	 * Loads the blocklist for the given player.
	 * @param player 玩家 / player
	 * @return 屏蔽名单 / block list
	 */
	BlockList load(Player player);

	/**
	 * 将目标对象 ID 加入指定玩家的黑名单。
	 * Adds the given object ID to the list of blocked players for the given player.
	 * @param playerObjId 被编辑黑名单的玩家 ID / ID of player whose blocklist is edited
	 * @param objIdToBlock 要加入黑名单的玩家 ID / ID of player to add to the blocklist
	 * @param reason 屏蔽原因 / block reason
	 * @return 是否成功 / whether successful
	 */
	boolean addBlockedUser(int playerObjId, int objIdToBlock, String reason);

	/**
	 * 从指定玩家的黑名单中删除目标对象 ID。
	 * Deletes the given object ID from the list of blocked players for the given player.
	 * @param playerObjId 被编辑黑名单的玩家 ID / ID of player whose blocklist is edited
	 * @param objIdToDelete 要从黑名单移除的玩家 ID / ID of player to remove from the blocklist
	 * @return 是否成功 / whether successful
	 */
	boolean delBlockedUser(int playerObjId, int objIdToDelete);

	/**
	 * 设置屏蔽某玩家的原因。
	 * Sets the reason for blocking a player.
	 * @param playerObjId 被编辑黑名单的玩家对象 ID / object ID of the player whose list is being edited
	 * @param blockedObjId 被屏蔽玩家的对象 ID / object ID of the player whose reason is being edited
	 * @param reason 要设置的原因 / the reason to be set
	 * @return 是否成功 / whether successful
	 */
	boolean setReason(int playerObjId, int blockedObjId, String reason);
}
