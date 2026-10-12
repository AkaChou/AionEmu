package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PlayerCooldownPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家冷却时间数据访问对象。
 * Player cooldowns data access object.
 * @author nrg
 */
public interface PlayerCooldownDao extends IService<PlayerCooldownPo> {

	/**
	 * 加载玩家冷却时间数据。
	 * Loads player cooldown data.
	 * @param player 玩家 / player
	 */
	void loadPlayerCooldowns(Player player);

	/**
	 * 存储玩家冷却时间数据。
	 * Stores player cooldown data.
	 * @param player 玩家 / player
	 */
	void storePlayerCooldowns(Player player);
}
