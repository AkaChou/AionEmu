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
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.model.templates.rift.RiftTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Source
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rift_locations")
public class RiftData {

	@XmlElement(name = "rift_location")
	private List<RiftTemplate> riftTemplates;
	@XmlTransient
	private Map<Integer, RiftLocation> rift = new LinkedHashMap<Integer, RiftLocation>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (RiftTemplate template : riftTemplates) {
			rift.put(template.getId(), new RiftLocation(template));
		}
	}

	public int size() {
		return rift.size();
	}

	public Map<Integer, RiftLocation> getRiftLocations() {
		return rift;
	}
}