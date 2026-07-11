package com.aionemu.gameserver.model.instance.instanceposition;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

/**
 * Genereal 副本坐标，用于副本相关逻辑。
 * Genereal Instance Position for instance logic.
 */

public class GenerealInstancePosition implements InstancePositionHandler {
	protected int mapId;
	protected int instanceId;

	/** 初始化 / initsialize. */
	@Override
	public void initsialize(Integer mapId, int instanceId) {
		this.mapId = mapId;
		this.instanceId = instanceId;
	}

	/** 端口 / port. */
	@Override
	public void port(Player player, int zone, int position) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	protected void teleport(Player player, float x, float y, float z, byte h) {
		TeleportService2.teleportTo(player, mapId, instanceId, x, y, z, h);
	}
}
