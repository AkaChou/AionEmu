package com.aionemu.gameserver.world.zone;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 飞行区域实例：进出时设置/清除 {@link ZoneType#FLY} 标记，并审计非法飞行离开。
 * Fly zone instance: sets/clears the {@link ZoneType#FLY} flag on enter/leave, and audits illegal flying leave.
 *
 * @author MrPoke
 */
public class FlyZoneInstance extends ZoneInstance {

	/**
	 * 创建飞行区域实例。
	 * Create a fly zone instance.
	 *
	 * map id
	 * @param template 区域模板信息 / zone template info
	 */
	public FlyZoneInstance(int mapId, ZoneInfo template) {
		super(mapId, template);
	}

	/**
	 * 进入飞行区并设置 FLY 区域类型。
	 * Enter fly zone and set the FLY zone type.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功进入 / whether enter succeeded
	 */
	@Override
	public synchronized boolean onEnter(Creature creature) {
		if (super.onEnter(creature)) {
			creature.setInsideZoneType(ZoneType.FLY);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 离开飞行区并清除 FLY 区域类型；若仍处于飞行状态则记录审计。
	 * Leave fly zone and clear the FLY zone type; audit if still flying.
	 *
	 * creature
	 *
	 * @param creature @return 是否成功离开 / whether leave succeeded
	 */
	@Override
	public synchronized boolean onLeave(Creature creature) {
		if (super.onLeave(creature)) {
			creature.unsetInsideZoneType(ZoneType.FLY);
			if (creature.isInState(CreatureState.FLYING) && !creature.isInState(CreatureState.FLIGHT_TELEPORT)) {
				if (creature instanceof Player) {
					AuditLogger.info((Player) creature, "On leave Fly zone in fly state!!");
				}
			}
			return true;
		} else {
			return false;
		}
	}
}
