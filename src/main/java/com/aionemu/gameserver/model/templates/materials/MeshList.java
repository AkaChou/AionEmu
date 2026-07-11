package com.aionemu.gameserver.model.templates.materials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Mesh 列表模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeshList", propOrder = { "meshMaterials" })
public class MeshList {

	@XmlElement(name = "mesh", required = true)
	protected List<MeshMaterial> meshMaterials;

	@XmlAttribute(name = "world_id", required = true)
	protected int worldId;

	@XmlTransient
	Map<String, Integer> materialIdsByPath = new HashMap<String, Integer>();

	@XmlTransient
	Map<Integer, String> pathZones = new HashMap<Integer, String>();

	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (meshMaterials == null) {
			return;
		}
		for (MeshMaterial meshMaterial : meshMaterials) {
			materialIdsByPath.put(meshMaterial.path, meshMaterial.materialId);
			pathZones.put(meshMaterial.path.hashCode(), meshMaterial.getZoneName());
			meshMaterial.path = null;
		}
		meshMaterials.clear();
		meshMaterials = null;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return worldId;
	}

	/**
	 * Find material ID for the specific mesh
	 *
	 * @param meshPath Mesh geo path
	 * @return 0 if not found
	 */
	public int getMeshMaterialId(String meshPath) {
		Integer materialId = materialIdsByPath.get(meshPath);
		if (materialId == null) {
			return 0;
		}
		return materialId;
	}

	/** 返回 mesh paths / Returns the mesh paths */
	public Set<String> getMeshPaths() {
		return materialIdsByPath.keySet();
	}

	/** 获取区域名称。 / Returns the zone name. */
	public String getZoneName(String meshPath) {
		return pathZones.get(meshPath.hashCode());
	}

	/** 大小 / size. */
	public int size() {
		return materialIdsByPath.size();
	}
}
