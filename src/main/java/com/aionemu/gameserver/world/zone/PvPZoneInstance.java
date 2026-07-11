package com.aionemu.gameserver.world.zone;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.model.templates.zone.ZoneType;

/**
 * PvP 区域实例：进出时设置/清除 {@link ZoneType#PVP} 标记。
 * PvP zone instance: sets/clears the {@link ZoneType#PVP} flag on enter/leave.
 */
public class PvPZoneInstance extends SiegeZoneInstance {

	/**
	 * 创建 PvP 区域实例。
	 * Create a PvP zone instance.
	 *
	 * map id
	 * @param template 区域模板信息 / zone template info
	 */
	public PvPZoneInstance(int mapId, ZoneInfo template) {
		super(mapId, template);
	}

	/**
	 * 进入 PvP 区并设置 PVP 区域类型。
	 * Enter PvP zone and set the PVP zone type.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功进入 / whether enter succeeded
	 */
	@Override
	public synchronized boolean onEnter(Creature creature) {
		if (super.onEnter(creature)) {
			creature.setInsideZoneType(ZoneType.PVP);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 离开 PvP 区并清除 PVP 区域类型。
	 * Leave PvP zone and clear the PVP zone type.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功离开 / whether leave succeeded
	 */
	@Override
	public synchronized boolean onLeave(Creature creature) {
		if (super.onLeave(creature)) {
			creature.unsetInsideZoneType(ZoneType.PVP);
			return true;
		} else {
			return false;
		}
	}
}
