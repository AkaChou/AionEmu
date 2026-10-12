package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.CraftCooldownPo;
import com.mybatisflex.core.service.IService;

/**
 * 制作冷却时间数据访问对象。
 * Craft cooldowns data access object.
 * @author synchro2
 */
public interface CraftCooldownDao extends IService<CraftCooldownPo> {

	/**
	 * 加载玩家的制作冷却时间。
	 * Loads craft cooldowns for a player.
	 * @param player 玩家 / player
	 */
	void loadCraftCooldowns(Player player);

	/**
	 * 存储玩家的制作冷却时间。
	 * Stores craft cooldowns for a player.
	 * @param player 玩家 / player
	 */
	void storeCraftCooldowns(Player player);
}
