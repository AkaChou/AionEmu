package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.EventItemPo;
import com.mybatisflex.core.service.IService;

/**
 * 活动物品数据访问对象。
 * Event items data access object.
 * @author wanke
 */
public interface EventItemDao extends IService<EventItemPo> {

	/**
	 * 加载玩家的活动物品。
	 * Loads event items for a player.
	 * @param player 玩家 / player
	 */
	void loadItems(Player player);

	/**
	 * 存储玩家的活动物品。
	 * Stores event items for a player.
	 * @param player 玩家 / player
	 */
	void storeItems(Player player);
}
