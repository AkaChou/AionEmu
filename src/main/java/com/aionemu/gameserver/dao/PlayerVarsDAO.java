package com.aionemu.gameserver.dao;

import java.util.Map;

import com.aionemu.commons.database.dao.DAO;

/**
 * 玩家自定义变量数据访问抽象层。
 * DAO for player custom key-value variables persistence.
 *
 * @author KID
 */
public abstract class PlayerVarsDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public String getClassName() {
		return PlayerVarsDAO.class.getName();
	}

	/**
	 * 加载玩家全部自定义变量。
	 * Loads all custom variables for the player.
	 *
	 * player object id
	 * @return 键值变量映射 / key-value variable map
	 */
	public abstract Map<String, Object> load(final int playerId);

	/**
	 * 设置玩家自定义变量。
	 * Sets a custom variable for the player.
	 *
	 * player object id
	 * variable key
	 * variable value
	 * @return 是否设置成功 / true if set
	 */
	public abstract boolean set(final int playerId, final String key, final Object value);

	/**
	 * 移除玩家自定义变量。
	 * Removes a custom variable for the player.
	 *
	 * player object id
	 * variable key
	 * @return 是否移除成功 / true if removed
	 */
	public abstract boolean remove(final int playerId, final String key);
}
