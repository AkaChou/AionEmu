package com.aionemu.gameserver.persistence.dao;

import java.util.Map;

import com.aionemu.gameserver.persistence.entity.PlayerVarPo;
import com.mybatisflex.core.service.IService;

/**
 * 玩家自定义变量数据访问层。
 * Data access layer for player custom key-value variables.
 */
public interface PlayerVarDao extends IService<PlayerVarPo> {

	/**
	 * 加载玩家全部自定义变量。
	 * Loads all custom variables for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @return 键值变量映射 / key-value variable map
	 */
	Map<String, Object> load(int playerId);

	/**
	 * 设置玩家自定义变量。
	 * Sets a custom variable for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param key 变量键 / variable key
	 * @param value 变量值 / variable value
	 * @return 是否设置成功 / true if set
	 */
	boolean set(int playerId, String key, Object value);

	/**
	 * 移除玩家自定义变量。
	 * Removes a custom variable for the player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param key 变量键 / variable key
	 * @return 是否移除成功 / true if removed
	 */
	boolean remove(int playerId, String key);
}
