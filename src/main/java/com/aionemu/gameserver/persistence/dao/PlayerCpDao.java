package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.cp.PlayerCPList;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerCpPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家创造点数（Creativity Points）数据访问层。
 * Data access layer for player creativity points.
 */
public interface PlayerCpDao extends IService<PlayerCpPo> {

	/**
	 * 加载玩家创造点数列表。
	 * Loads the player's creativity-points list.
	 * @param paramPlayer 玩家 / player
	 * @return 创造点数列表 / creativity-points list
	 */
	PlayerCPList loadCP(Player paramPlayer);

	/**
	 * 存储一条创造点数记录。
	 * Stores a creativity-points entry.
	 * @param paramInt1 玩家对象 ID / player object id
	 * @param paramInt2 槽位或条目 ID / slot or entry id
	 * @param paramInt3 点数值 / points value
	 */
	void storeCP(int paramInt1, int paramInt2, int paramInt3);

	/**
	 * 删除指定槽位的创造点数记录。
	 * Deletes a creativity-points entry for the given slot.
	 * @param playerObjId 玩家对象 ID / player object id
	 * @param slot 槽位 / slot
	 */
	void deleteCP(int playerObjId, int slot);

	/**
	 * 获取玩家已用创造点数槽位数。
	 * Returns the number of creativity-points slots used by the player.
	 * @param playerId 玩家 ID / player id
	 * @return 槽位数 / slot count
	 */
	int getSlotSize(int playerId);
}
