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
package com.aionemu.gameserver.model.assemblednpc;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 *
 * @author xTz
 */
public class AssembledNpc {

	@Getter
	private List<AssembledNpcPart> assembledParts = new ArrayList<>();
	private long spawnTime = System.currentTimeMillis();
	@Getter
	private int routeId;
	@Getter
	private int mapId;

	public AssembledNpc(int routeId, int mapId, int liveTime, List<AssembledNpcPart> assembledParts) {
		this.assembledParts = new ArrayList<>(assembledParts);
		this.routeId = routeId;
		this.mapId = mapId;
	}

	public long getTimeOnMap() {
		return System.currentTimeMillis() - spawnTime;
	}
}
