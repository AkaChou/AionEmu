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

import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LegionContainer implements Iterable<Legion> {
	private final Map<Integer, Legion> legionsById = new LinkedHashMap<Integer, Legion>();
	private final Map<String, Legion> legionsByName = new LinkedHashMap<String, Legion>();

	public synchronized void add(Legion legion) {
		if (legion == null || legion.getLegionName() == null) {
			return;
		}
		String legionName = legion.getLegionName().toLowerCase();
		if (legionsById.containsKey(legion.getLegionId()) || legionsByName.containsKey(legionName)) {
			throw new DuplicateAionObjectException();
		}
		legionsById.put(legion.getLegionId(), legion);
		legionsByName.put(legionName, legion);
	}

	public synchronized void remove(Legion legion) {
		legionsById.remove(legion.getLegionId());
		legionsByName.remove(legion.getLegionName().toLowerCase());
	}

	public synchronized Legion get(int legionId) {
		return legionsById.get(legionId);
	}

	public synchronized Legion get(String name) {
		return legionsByName.get(name.toLowerCase());
	}

	public synchronized List<Legion> getAllLegions() {
		return new ArrayList<Legion>(legionsByName.values());
	}

	public synchronized boolean contains(int legionId) {
		return legionsById.containsKey(legionId);
	}

	public synchronized boolean contains(String name) {
		return legionsByName.containsKey(name.toLowerCase());
	}

	@Override
	public synchronized Iterator<Legion> iterator() {
		return new ArrayList<Legion>(legionsById.values()).iterator();
	}

	public synchronized void clear() {
		legionsById.clear();
		legionsByName.clear();
	}
}
