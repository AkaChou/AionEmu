package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerTransformPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家变身面板数据访问层。
 * Data access layer for player transformation panel persistence.
 */
public interface PlayerTransformDao extends IService<PlayerTransformPo> {

	/**
	 * 加载玩家变身数据。
	 * Loads transformation data for the player.
	 * @param player 玩家 / player
	 */
	void loadPlTransfo(Player player);

	/**
	 * 保存玩家变身面板数据。
	 * Stores transformation panel data for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param panelId 面板 ID / panel id
	 * @param itemId 物品 ID / item id
	 * @return 是否保存成功 / true if stored
	 */
	boolean storePlTransfo(int playerId, int panelId, int itemId);

	/**
	 * 删除玩家变身数据。
	 * Deletes transformation data for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 是否删除成功 / true if deleted
	 */
	boolean deletePlTransfo(int playerId);
}
