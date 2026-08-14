package com.aionemu.gameserver.world.zone.scripts.pvpZones;

import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.zone.ZoneName;
import com.aionemu.gameserver.world.zone.handler.ZoneNameAnnotation;

/**
 * 具体 PvP 区域：圣所 / 伏魔殿子区域死亡后传送点。
 * Sanctum / Pandaemonium PvP sub-zones: death teleport points.
 */
@ZoneNameAnnotation(value = "LC1_PVP_SUB_C DC1_PVP_ZONE")
public class PvPAreaZone extends PvPZone {

	/**
	 * 按区域名称传送到对应复活点。
	 * Teleport to the matching revive point by zone name.
	 *
	 * @param player 玩家 / player
	 * @param zoneName 区域名称 / zone name
	 */
	@Override
	protected void doTeleport(Player player, ZoneName zoneName) {
		if (zoneName == ZoneName.get("LC1_PVP_SUB_C")) {
			TeleportService2.teleportTo(player, 110010000, 1465.1226f, 1336.6649f, 566.41583f, (byte) 92);
		} else if (zoneName == ZoneName.get("DC1_PVP_ZONE")) {
			TeleportService2.teleportTo(player, 120010000, 1004.49927f, 1528.2157f, 222.19403f, (byte) 52);
		}
	}
}
