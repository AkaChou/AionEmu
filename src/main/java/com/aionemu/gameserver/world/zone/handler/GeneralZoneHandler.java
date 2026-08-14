package com.aionemu.gameserver.world.zone.handler;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 空实现的通用区域处理器（占位 / 默认处理器）。
 * General zone handler with empty implementations (placeholder / default handler).
 *
 * @author MrPoke
 */
public class GeneralZoneHandler implements ZoneHandler {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onEnterZone(Creature player, ZoneInstance zone) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLeaveZone(Creature player, ZoneInstance zone) {
	}
}
