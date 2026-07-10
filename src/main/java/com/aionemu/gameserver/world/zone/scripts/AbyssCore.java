/*
 * This file is part of Encom.
 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.world.zone.scripts;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.controllers.observer.CollisionDieActor;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;
import com.aionemu.gameserver.world.zone.handler.ZoneNameAnnotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ZoneNameAnnotation("CORE_400010000")
@Slf4j
public class AbyssCore implements ZoneHandler
{
	private static final String CORE_GEOMETRY = "levels/common/abyss/abground/landmark/ground_a/na_ab_lmark_col_01a.cgf";

	Map<Integer, CollisionDieActor> observed = new ConcurrentHashMap<Integer, CollisionDieActor>();
	
	private final Spatial geometry;
	
	public AbyssCore() {
		geometry = GeoService.getInstance().getGeometry(400010000, CORE_GEOMETRY);
		if (geometry == null) {
			log.error("Abyss core geometry is missing from 400010000.geo: {}", CORE_GEOMETRY);
		}
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		Creature acting = creature.getActingCreature();
		if (geometry != null && acting instanceof Player && !((Player) acting).isGM()) {
			CollisionDieActor observer = new CollisionDieActor(creature, geometry);
			creature.getObserveController().addObserver(observer);
			observed.put(creature.getObjectId(), observer);
		}
	}
	
	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		Creature acting = creature.getActingCreature();
		if (acting instanceof Player && !((Player) acting).isGM()) {
			CollisionDieActor observer = observed.get(creature.getObjectId());
			if (observer != null) {
				creature.getObserveController().removeObserver(observer);
				observed.remove(creature.getObjectId());
			}
		}
	}
}
