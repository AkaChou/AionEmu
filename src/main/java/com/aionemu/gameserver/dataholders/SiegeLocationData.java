package com.aionemu.gameserver.dataholders;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

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

/**
 * 攻城据点数据容器，按类型分索引要塞、神器与统一据点映射。
 * Siege location data holder, indexing fortresses, artifacts and the unified location map by type.
 */
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

	/**
	 * JAXB 反序列化完成后，按类型构建要塞/神器/统一据点索引。
	 * After JAXB unmarshalling, builds fortress/artifact/unified location indexes by type.
	 */
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

	/**
	 * 返回统一攻城据点数量。
	 * Returns the number of unified siege locations.
	 *
	 * location count
	 */
	public int size() {
		return siegeLocations.size();
	}

	/**
	 * 返回神器据点映射。
	 * Returns the artifact location map.
	 *
	 * @return ID 到神器的映射 / map of id to artifact
	 */
	public Map<Integer, ArtifactLocation> getArtifacts() {
		return artifactLocations;
	}

	/**
	 * 返回要塞据点映射。
	 * Returns the fortress location map.
	 *
	 * @return ID 到要塞的映射 / map of id to fortress
	 */
	public Map<Integer, FortressLocation> getFortress() {
		return fortressLocations;
	}

	/**
	 * 返回全部攻城据点映射。
	 * Returns the full siege location map.
	 *
	 * @return ID 到据点的映射 / map of id to location
	 */
	public Map<Integer, SiegeLocation> getSiegeLocations() {
		return siegeLocations;
	}
}
