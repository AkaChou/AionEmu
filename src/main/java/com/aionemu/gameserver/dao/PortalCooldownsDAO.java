package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家传送门冷却数据访问抽象层。
 * DAO for player portal cooldown persistence.
 */
public abstract class PortalCooldownsDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PortalCooldownsDAO.class.getName();
	}

	/**
	 * 加载玩家传送门冷却数据。
	 * Loads portal cooldowns for the player.
	 *
	 * 玩家 / player
	 */
	public abstract void loadPortalCooldowns(Player player);

	/**
	 * 保存玩家传送门冷却数据。
	 * Stores portal cooldowns for the player.
	 *
	 * 玩家 / player
	 */
	public abstract void storePortalCooldowns(Player player);
}
