package com.aionemu.gameserver.persistence.dao;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.persistence.entity.PortalCooldownPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家传送门冷却数据访问抽象层。
 * DAO for player portal cooldown persistence.
 */
public interface PortalCooldownDao extends IService<PortalCooldownPo> {

	/**
	 * 加载玩家传送门冷却数据。
	 * Loads portal cooldowns for the player.
	 * @param player 玩家 / player
	 */
	void loadPortalCooldowns(Player player);

	/**
	 * 保存玩家传送门冷却数据。
	 * Stores portal cooldowns for the player.
	 * @param player 玩家 / player
	 */
	void storePortalCooldowns(Player player);
}
