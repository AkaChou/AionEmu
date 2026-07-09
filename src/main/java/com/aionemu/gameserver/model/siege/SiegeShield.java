/*

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
package com.aionemu.gameserver.model.siege;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.CollisionDieActor;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;

/**
 * Shields have material ID 11 in geo.
 * 
 * @author Rolandas
 */
public class SiegeShield implements ZoneHandler {

	Map<Integer, ActionObserver> observed = new ConcurrentHashMap<Integer, ActionObserver>();
	private Spatial geometry;
	private int siegeLocationId;
	private boolean isEnabled = false;

	public SiegeShield(Spatial geometry) {
		this.geometry = geometry;
		if (geometry.getParent() instanceof DespawnableNode) {
			((DespawnableNode) geometry.getParent()).setType(DespawnableNode.DespawnableType.SHIELD);
		}
	}

	public Spatial getGeometry() {
		return geometry;
	}

	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		if (!(creature instanceof Player)) {
			return;
		}
		Player player = (Player) creature;
		if (GeoDataConfig.GEO_SHIELDS_ENABLE && (isEnabled || siegeLocationId == 0)) {
			FortressLocation loc = GameFeatureServices.siegeService().getFortress(siegeLocationId);
			if (loc == null || loc.getRace() != SiegeRace.getByRace(player.getRace())) {
				CollisionDieActor actor = new CollisionDieActor(creature, geometry);
				creature.getObserveController().addObserver(actor);
				observed.put(creature.getObjectId(), actor);
			}
		}
	}

	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		ActionObserver actor = observed.get(creature.getObjectId());
		if (actor != null) {
			creature.getObserveController().removeObserver(actor);
			observed.remove(creature.getObjectId());
		}
	}

	public void setEnabled(boolean enable) {
		isEnabled = enable;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public int getSiegeLocationId() {
		return siegeLocationId;
	}

	public void setSiegeLocationId(int siegeLocationId) {
		this.siegeLocationId = siegeLocationId;
		if (geometry.getParent() instanceof DespawnableNode) {
			((DespawnableNode) geometry.getParent()).setId(siegeLocationId);
		}
	}

	@Override
	public String toString() {
		return "LocId=" + siegeLocationId + "; Name=" + geometry.getName() + "; Bounds=" + geometry.getWorldBound();
	}
}
