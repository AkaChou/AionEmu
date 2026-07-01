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

import com.aionemu.gameserver.model.siege.ArtifactLocation;
import com.aionemu.gameserver.model.siege.FortressLocation;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeLocationTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@XmlRootElement(name = "siege_locations")
@XmlAccessorType(XmlAccessType.FIELD)
public class SiegeLocationData {
	@XmlElement(name = "siege_location")
	private List<SiegeLocationTemplate> siegeLocationTemplates;

	@XmlTransient
	private Map<Integer, ArtifactLocation> artifactLocations = new LinkedHashMap<Integer, ArtifactLocation>();
	@XmlTransient
	private Map<Integer, FortressLocation> fortressLocations = new LinkedHashMap<Integer, FortressLocation>();
	@XmlTransient
	private Map<Integer, SiegeLocation> siegeLocations = new LinkedHashMap<Integer, SiegeLocation>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		artifactLocations.clear();
		fortressLocations.clear();
		siegeLocations.clear();
		for (SiegeLocationTemplate template : siegeLocationTemplates) {
			switch (template.getType()) {
			case FORTRESS:
				FortressLocation fortress = new FortressLocation(template);
				fortressLocations.put(template.getId(), fortress);
				siegeLocations.put(template.getId(), fortress);
				artifactLocations.put(template.getId(), new ArtifactLocation(template));
				break;
			case ARTIFACT:
				ArtifactLocation artifact = new ArtifactLocation(template);
				artifactLocations.put(template.getId(), artifact);
				siegeLocations.put(template.getId(), artifact);
				break;
			default:
				break;
			}
		}
	}

	public int size() {
		return siegeLocations.size();
	}

	public Map<Integer, ArtifactLocation> getArtifacts() {
		return artifactLocations;
	}

	public Map<Integer, FortressLocation> getFortress() {
		return fortressLocations;
	}

	public Map<Integer, SiegeLocation> getSiegeLocations() {
		return siegeLocations;
	}
}