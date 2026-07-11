package com.aionemu.gameserver.world.zone.handler;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 区域进入/离开事件处理器接口。
 * Handler interface for zone enter/leave events.
 *
 * @author MrPoke
 */
public interface ZoneHandler {

	/**
	 * 生物进入区域时回调。
	 * Called when a creature enters the zone.
	 *
	 * @param player 进入的生物 / creature that entered
	 * @param zone   区域实例 / zone instance
	 */
	void onEnterZone(Creature player, ZoneInstance zone);

	/**
	 * 生物离开区域时回调。
	 * Called when a creature leaves the zone.
	 *
	 * @param player 离开的生物 / creature that left
	 * @param zone   区域实例 / zone instance
	 */
	void onLeaveZone(Creature player, ZoneInstance zone);
}
