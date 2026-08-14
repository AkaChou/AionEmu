package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 玩家 UI/客户端设置数据访问抽象层。
 * DAO for player UI/client settings persistence.
 *
 * @author ATracer
 */
public abstract class PlayerSettingsDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * @return 完整类名 / fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return PlayerSettingsDAO.class.getName();
	}

	/**
	 * 保存玩家设置。
	 * Saves player settings.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void saveSettings(final Player player);

	/**
	 * 加载玩家设置。
	 * Loads player settings.
	 *
	 * @param player 玩家 / player
	 */
	public abstract void loadSettings(final Player player);
}
