package com.aionemu.gameserver.model.instance.instanceposition;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 副本坐标处理器。
 * Instance Position Handler interface.
 */

public interface InstancePositionHandler {

	/**
	 * 初始化副本坐标处理器。
	 * Initializes the instance position handler.
	 *
	 * @param mapId 地图 ID / map id
	 * @param instanceId 副本实例 ID / instance id
	 */
	void initsialize(Integer mapId, int instanceId);

	/**
	 * 将玩家传送到指定区域与位置。
	 * Ports the player to the given zone and position.
	 *
	 * @param player 目标玩家 / target player
	 * @param zone 区域 / zone
	 * @param position 位置 / position
	 */
	void port(Player player, int zone, int position);
}
