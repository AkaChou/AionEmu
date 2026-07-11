package com.aionemu.gameserver.model.instance.instanceposition;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 副本坐标处理器。
 * Instance Position Handler interface.
 */

public interface InstancePositionHandler {
	void initsialize(Integer mapId, int instanceId);

	void port(Player player, int zone, int position);
}
