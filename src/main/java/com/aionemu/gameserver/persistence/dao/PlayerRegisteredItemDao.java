package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.house.HouseRegistry;
import com.aionemu.gameserver.persistence.entity.PlayerRegisteredItemPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家房屋注册物品数据访问对象。
 * DAO for player house registered items persistence.
 */
public interface PlayerRegisteredItemDao extends IService<PlayerRegisteredItemPo> {

	/**
	 * 加载玩家房屋注册表。
	 * Loads the house registry for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 */
	void loadRegistry(int playerId);

	/**
	 * 保存玩家房屋注册表。
	 * Stores the house registry for the player.
	 *
	 * @param registry 房屋注册表 / house registry
	 * @param playerId 玩家对象 ID / player object id
	 * @return 是否保存成功 / true if stored
	 */
	boolean store(HouseRegistry registry, int playerId);

	/**
	 * 重置玩家房屋注册表。
	 * Resets the house registry for the player.
	 *
	 * @param playerId 玩家对象 ID / player object id
	 */
	void resetRegistry(int playerId);

	/**
	 * 返回本 DAO 已使用的全部 ID。
	 * Returns an array of all IDs that are used by this DAO.
	 * @return 已使用 ID 数组 / array of used IDs
	 */
	int[] getUsedIDs();
}
