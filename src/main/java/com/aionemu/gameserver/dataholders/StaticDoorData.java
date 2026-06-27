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

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.staticdoor.StaticDoorWorld;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * @author Wakizashi
 */
@XmlRootElement(name = "staticdoor_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class StaticDoorData {

	@XmlElement(name = "world")
	private List<StaticDoorWorld> staticDorWorlds;

	/** A map containing all door templates */
	private IntObjectHashMap<StaticDoorWorld> staticDoorData = new IntObjectHashMap<StaticDoorWorld>();

	/**
	 * @param u
	 * @param parent
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		staticDoorData.clear();

		for (StaticDoorWorld world : staticDorWorlds) {
			staticDoorData.put(world.getWorld(), world);
		}
	}

	public int size() {
		return staticDoorData.size();
	}

	/**
	 * @param world
	 * @return
	 */
	public StaticDoorWorld getStaticDoorWorlds(int world) {
		return staticDoorData.get(world);
	}

	/**
	 * @return the staticDorWorlds
	 */
	public List<StaticDoorWorld> getStaticDorWorlds() {
		return staticDorWorlds;
	}

	/**
	 *
	 */
	public void setStaticDorWorlds(List<StaticDoorWorld> staticDorWorlds) {
		this.staticDorWorlds = staticDorWorlds;
		afterUnmarshal(null, null);
	}
}