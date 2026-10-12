package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.ItemCooldownPo;
import com.mybatisflex.core.service.IService;

/**
 * 物品冷却时间数据访问对象。
 * Item cooldowns data access object.
 * @author ATracer
 */
public interface ItemCooldownDao extends IService<ItemCooldownPo> {

	/**
	 * 加载玩家的物品冷却时间。
	 * Loads item cooldowns for a player.
	 * @param player 玩家 / player
	 */
	void loadItemCooldowns(Player player);

	/**
	 * 存储玩家的物品冷却时间。
	 * Stores item cooldowns for a player.
	 * @param player 玩家 / player
	 */
	void storeItemCooldowns(Player player);
}
