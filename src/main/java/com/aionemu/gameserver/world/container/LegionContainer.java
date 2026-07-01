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
package com.aionemu.gameserver.world.container;

import java.util.Iterator;
import java.util.Collections;

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LegionContainer implements Iterable<Legion> {
	private final Map<Integer, Legion> legionsById = Collections.synchronizedMap(new LinkedHashMap<Integer, Legion>());
	private final Map<String, Legion> legionsByName = Collections.synchronizedMap(new LinkedHashMap<String, Legion>());

	public void add(Legion legion) {
		if (legion == null || legion.getLegionName() == null) {
			return;
		}
		if (legionsById.put(legion.getLegionId(), legion) != null) {
			throw new DuplicateAionObjectException();
		}
		if (legionsByName.put(legion.getLegionName().toLowerCase(), legion) != null) {
			throw new DuplicateAionObjectException();
		}
	}

	public void remove(Legion legion) {
		legionsById.remove(legion.getLegionId());
		legionsByName.remove(legion.getLegionName().toLowerCase());
	}

	public Legion get(int legionId) {
		return legionsById.get(legionId);
	}

	public Legion get(String name) {
		return legionsByName.get(name.toLowerCase());
	}

	public List<Legion> getAllLegions() {
		synchronized (legionsByName) {
			return new ArrayList<Legion>(legionsByName.values());
		}
	}

	public boolean contains(int legionId) {
		return legionsById.containsKey(legionId);
	}

	public boolean contains(String name) {
		return legionsByName.containsKey(name.toLowerCase());
	}

	@Override
	public Iterator<Legion> iterator() {
		synchronized (legionsById) {
			return new ArrayList<Legion>(legionsById.values()).iterator();
		}
	}

	public void clear() {
		legionsById.clear();
		legionsByName.clear();
	}
}
