package com.aionemu.gameserver.world.zone.handler;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 扩展区域处理器：在进入/离开之外，还可处理区域内死亡事件。
 * Extended zone handler: besides enter/leave, can also handle death events inside the zone.
 *
 * @author MrPoke
 */
public interface AdvencedZoneHandler extends ZoneHandler {

	/**
	 * 生物在区域内死亡时回调。
	 * Called when a creature dies inside the zone.
	 *
	 * @param attacker 攻击者 / attacker
	 * @param target 死亡目标 / dead target
	 * @param zone 区域实例 / zone instance
	 * @return 是否已处理该死亡事件 / whether the death event was handled
	 */
	public boolean onDie(Creature attacker, Creature target, ZoneInstance zone);
}
