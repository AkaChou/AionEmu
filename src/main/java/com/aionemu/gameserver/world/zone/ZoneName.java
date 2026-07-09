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
package com.aionemu.gameserver.world.zone;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rolandas
 */
@Slf4j
public final class ZoneName {


	private static final Map<String, ZoneName> zoneNames = new ConcurrentHashMap<String, ZoneName>();
	public static final String NONE = "NONE";
	public static final String ABYSS_CASTLE = "_ABYSS_CASTLE_AREA_";

	static {
		zoneNames.put(NONE, new ZoneName(NONE));
		zoneNames.put(ABYSS_CASTLE, new ZoneName(ABYSS_CASTLE));
	}

	private String _name;

	private ZoneName(String name) {
		this._name = name;
	}

	public String name() {
		return _name;
	}

	public int id() {
		return _name.hashCode();
	}

	public static final ZoneName createOrGet(String name) {
		return zoneNames.computeIfAbsent(name.toUpperCase(), ZoneName::new);
	}

	public static final int getId(String name) {
		ZoneName zoneName = zoneNames.get(name.toUpperCase());
		return (zoneName != null ? zoneName : zoneNames.get(NONE)).id();
	}

	public static final ZoneName get(String name) {
		name = name.toUpperCase();
		ZoneName zoneName = zoneNames.get(name);
		if (zoneName != null) {
			return zoneName;
		}
		log.warn("Missing zone : " + name);
		return zoneNames.get(NONE);
	}

	@Override
	public String toString() {
		return _name;
	}
}
