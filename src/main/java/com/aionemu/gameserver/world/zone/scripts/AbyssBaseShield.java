package com.aionemu.gameserver.world.zone.scripts;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;
import com.aionemu.gameserver.world.zone.handler.ZoneNameAnnotation;

/**
 * 欧比斯基地护盾区：敌对种族进入时立即死亡（GM 除外）。
 * Abyss base shield zone: hostile race dies on enter (GMs exempt).
 */
@ZoneNameAnnotation("PRIMUM_FORTRESS TERMINON_LANDING")
public class AbyssBaseShield implements ZoneHandler {

	/**
	 * 进入护盾区：非 GM 敌对种族立即死亡。
	 * Enter shield zone: non-GM hostile race dies immediately.
	 *
	 * creature
	 * @param zone     区域实例 / zone instance
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		Creature actingCreature = creature.getActingCreature();
		if (actingCreature instanceof Player && !((Player) actingCreature).isGM()) {
			ZoneName currZone = zone.getZoneTemplate().getName();
			if (currZone == ZoneName.get("PRIMUM_FORTRESS")) {
				if (((Player) actingCreature).getRace() == Race.ELYOS) {
					creature.getController().die();
				}
			} else if (currZone == ZoneName.get("TERMINON_LANDING")) {
				if (((Player) actingCreature).getRace() == Race.ASMODIANS) {
					creature.getController().die();
				}
			}
		}
	}

	/**
	 * 离开护盾区：无额外逻辑。
	 * Leave shield zone: no additional logic.
	 *
	 * creature
	 * @param zone   区域实例 / zone instance
	 */
	@Override
	public void onLeaveZone(Creature player, ZoneInstance zone) {
	}
}
