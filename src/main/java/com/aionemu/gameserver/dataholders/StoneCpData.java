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
package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.panel_cp.StoneCP;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * Created by Rinzler (Encom)
 */
@XmlRootElement(name = "stones_cp")
@XmlAccessorType(XmlAccessType.FIELD)
public class StoneCpData {
	@XmlElement(name = "stone_cp")
	private List<StoneCP> stonelist;

	@XmlTransient
	private IntObjectHashMap<StoneCP> stoneData = new IntObjectHashMap<StoneCP>();

	@XmlTransient
	private Map<Integer, StoneCP> stoneDataMap = new HashMap<Integer, StoneCP>(1);

	void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
		for (StoneCP stoneCp : stonelist) {
			stoneData.put(stoneCp.getId(), stoneCp);
			stoneDataMap.put(stoneCp.getId(), stoneCp);
		}
	}

	public int size() {
		return stoneData.size();
	}

	public StoneCP getStoneCpId(int id) {
		return stoneData.get(id);
	}

	public Map<Integer, StoneCP> getAll() {
		return stoneDataMap;
	}
}