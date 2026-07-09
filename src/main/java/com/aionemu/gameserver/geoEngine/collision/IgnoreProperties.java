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
package com.aionemu.gameserver.geoEngine.collision;

import com.aionemu.gameserver.model.Race;

public class IgnoreProperties {

	public static final IgnoreProperties ELYOS = new IgnoreProperties(Race.ELYOS, 0);
	public static final IgnoreProperties ASMODIANS = new IgnoreProperties(Race.ASMODIANS, 0);
	public static final IgnoreProperties BALAUR = new IgnoreProperties(Race.DRAKAN, 0);
	public static final IgnoreProperties ANY_RACE = new IgnoreProperties(null, 0);

	private final Race race;
	private final int staticId;

	private IgnoreProperties(Race race, int staticId) {
		this.race = race;
		this.staticId = staticId;
	}

	public static IgnoreProperties of(Race race, int staticId) {
		if (staticId == 0) {
			if (race == Race.ELYOS) {
				return ELYOS;
			}
			if (race == Race.ASMODIANS) {
				return ASMODIANS;
			}
			if (race == Race.DRAKAN) {
				return BALAUR;
			}
		}
		return new IgnoreProperties(race, staticId);
	}

	public static IgnoreProperties of(Race race) {
		return of(race, 0);
	}

	public static IgnoreProperties of(int staticId) {
		return of(null, staticId);
	}

	public Race getRace() {
		return race;
	}

	public int getStaticId() {
		return staticId;
	}

	@Override
	public String toString() {
		return "[IgnoreProperties] Race: " + race + " staticId: " + staticId;
	}
}
