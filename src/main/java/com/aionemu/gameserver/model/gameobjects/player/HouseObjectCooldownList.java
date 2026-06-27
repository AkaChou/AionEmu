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
package com.aionemu.gameserver.model.gameobjects.player;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rolandas
 */
public class HouseObjectCooldownList {

	private Map<Integer, Long> houseObjectCooldowns;

	HouseObjectCooldownList(Player owner) {
	}

	public boolean isCanUseObject(int objectId) {
		if (houseObjectCooldowns == null || !houseObjectCooldowns.containsKey(objectId)) {
			return true;
		}
		Long coolDown = houseObjectCooldowns.get(objectId);
		if (coolDown == null) {
			return true;
		}
		if (coolDown < System.currentTimeMillis()) {
			houseObjectCooldowns.remove(objectId);
			return true;
		}
		return false;
	}

	public long getHouseObjectCooldown(int objectId) {
		if (houseObjectCooldowns == null || !houseObjectCooldowns.containsKey(objectId)) {
			return 0;
		}
		return houseObjectCooldowns.get(objectId);
	}

	public Map<Integer, Long> getHouseObjectCooldowns() {
		return houseObjectCooldowns;
	}

	public void setHouseObjectCooldowns(Map<Integer, Long> houseObjectCooldowns) {
		this.houseObjectCooldowns = houseObjectCooldowns;
	}

	public void addHouseObjectCooldown(int objectId, int delay) {
		if (houseObjectCooldowns == null) {
			houseObjectCooldowns = new HashMap<Integer, Long>();
		}

		long nextUseTime = System.currentTimeMillis() + (delay * 1000);
		houseObjectCooldowns.put(objectId, nextUseTime);
	}

	public int getReuseDelay(int objectId) {
		if (isCanUseObject(objectId)) {
			return 0;
		}
		long cd = getHouseObjectCooldown(objectId);
		int delay = (int) ((cd - System.currentTimeMillis()) / 1000);
		return delay;
	}
}
