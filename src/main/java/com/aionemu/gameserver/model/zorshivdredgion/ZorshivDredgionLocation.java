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
package com.aionemu.gameserver.model.zorshivdredgion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.zorshivdredgion.ZorshivDredgionTemplate;
import com.aionemu.gameserver.services.zorshivdredgionservice.ZorshivDredgion;

/**
 * @author Rinzler (Encom)
 */

public class ZorshivDredgionLocation {
	protected int id;
	protected boolean isActive;
	protected ZorshivDredgionTemplate template;
	protected ZorshivDredgion<ZorshivDredgionLocation> activeZorshivDredgion;
	protected Map<Integer, Player> players = new HashMap<>();
	private final List<VisibleObject> spawned = new ArrayList<VisibleObject>();

	public ZorshivDredgionLocation() {
	}

	public ZorshivDredgionLocation(ZorshivDredgionTemplate template) {
		this.template = template;
		this.id = template.getId();
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActiveZorshivDredgion(ZorshivDredgion<ZorshivDredgionLocation> zorshivDredgion) {
		isActive = zorshivDredgion != null;
		this.activeZorshivDredgion = zorshivDredgion;
	}

	public ZorshivDredgion<ZorshivDredgionLocation> getActiveZorshivDredgion() {
		return activeZorshivDredgion;
	}

	public final ZorshivDredgionTemplate getTemplate() {
		return template;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return template.getName();
	}

	public List<VisibleObject> getSpawned() {
		return spawned;
	}

	public Map<Integer, Player> getPlayers() {
		return players;
	}
}
