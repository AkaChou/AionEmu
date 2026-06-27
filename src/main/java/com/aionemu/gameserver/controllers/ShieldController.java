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
package com.aionemu.gameserver.controllers;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.shield.Shield;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.services.ShieldService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShieldController extends VisibleObjectController<Shield> {
	Map<Integer, ActionObserver> observed = new LinkedHashMap<Integer, ActionObserver>();

	@Override
	public void see(VisibleObject object) {
		FortressLocation loc = GameFeatureServices.siegeService().getFortress(getOwner().getId());
		Player player = (Player) object;
		if (loc.isUnderShield()) {
			if (loc.getRace() != SiegeRace.getByRace(player.getRace())) {
				ActionObserver observer = GameFeatureServices.shieldService().createShieldObserver(loc.getLocationId(), player);
				if (observer != null) {
					player.getObserveController().addObserver(observer);
					observed.put(player.getObjectId(), observer);
				}
			}
		}
	}

	@Override
	public void notSee(VisibleObject object, boolean isOutOfRange) {
		FortressLocation loc = GameFeatureServices.siegeService().getFortress(getOwner().getId());
		Player player = (Player) object;
		if (loc.isUnderShield()) {
			if (loc.getRace() != SiegeRace.getByRace(player.getRace())) {
				ActionObserver observer = observed.remove(player.getObjectId());
				if (observer != null) {
					if (isOutOfRange)
						observer.moved();
					player.getObserveController().removeObserver(observer);
				}
			}
		}
	}

	public void disable() {
		for (Integer playerId : observed.keySet().toArray(Integer[]::new)) {
			ActionObserver observer = observed.remove(playerId);
			Player player = World.getInstance().findPlayer(playerId);
			if (player != null) {
				player.getObserveController().removeObserver(observer);
			}
		}
	}
}
