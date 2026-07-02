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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.model.templates.vortex.VortexTemplate;
import com.aionemu.gameserver.model.vortex.VortexLocation;

/**
 * @author Source
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "dimensional_vortex")
public class VortexData {

	@XmlElement(name = "vortex_location")
	private List<VortexTemplate> vortexTemplates;
	@XmlTransient
	private Map<Integer, VortexLocation> vortex = new LinkedHashMap<Integer, VortexLocation>();
	@XmlTransient
	private Map<Integer, VortexLocation> vortexByInvasionWorldId = new HashMap<Integer, VortexLocation>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		for (VortexTemplate template : vortexTemplates) {
			VortexLocation location = new VortexLocation(template);
			vortex.put(template.getId(), location);
			vortexByInvasionWorldId.putIfAbsent(location.getInvasionWorldId(), location);
		}
	}

	public int size() {
		return vortex.size();
	}

	public VortexLocation getVortexLocation(int invasionWorldId) {
		return vortexByInvasionWorldId.get(invasionWorldId);
	}

	public Map<Integer, VortexLocation> getVortexLocations() {
		return vortex;
	}
}
