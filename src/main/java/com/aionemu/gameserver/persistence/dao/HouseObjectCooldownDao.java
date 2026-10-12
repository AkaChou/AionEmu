package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.HouseObjectCooldownPo;
import com.mybatisflex.core.service.IService;

/**
 * 房屋物件冷却时间数据访问对象。
 * House object cooldowns data access object.
 */
public interface HouseObjectCooldownDao extends IService<HouseObjectCooldownPo> {

	/**
	 * 加载玩家的房屋物件冷却时间。
	 * Loads house object cooldowns for a player.
	 * @param paramPlayer 玩家 / player
	 */
	void loadHouseObjectCooldowns(Player paramPlayer);

	/**
	 * 存储玩家的房屋物件冷却时间。
	 * Stores house object cooldowns for a player.
	 * @param paramPlayer 玩家 / player
	 */
	void storeHouseObjectCooldowns(Player paramPlayer);
}
