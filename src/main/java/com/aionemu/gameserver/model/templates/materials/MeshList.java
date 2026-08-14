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
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeshList", propOrder = { "meshMaterials" })
public class MeshList {

	/** 网格材料列表。 / Mesh material list. */
	@XmlElement(name = "mesh", required = true)
	protected List<MeshMaterial> meshMaterials;

	/** 世界 ID。 / World id. */
	@XmlAttribute(name = "world_id", required = true)
	protected int worldId;

	/** 网格路径到材料 ID 的映射。 / Map of mesh path to material id. */
	@XmlTransient
	Map<String, Integer> materialIdsByPath = new HashMap<String, Integer>();

	/** 路径哈希到区域名称的映射。 / Map of path hash to zone name. */
	@XmlTransient
	Map<Integer, String> pathZones = new HashMap<Integer, String>();

	/**
	 * JAXB 反序列化后处理：将网格材料整理为查询映射并释放中间列表。
	 * Post-unmarshal processing: reorganizes mesh materials into lookup maps and releases the intermediate list.
	 */
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
	 * 查找指定网格路径对应的材料 ID。
	 * Finds the material ID for the specific mesh.
	 *
	 * @param meshPath 网格几何路径 / Mesh geo path
	 * @return 材料 ID，未找到时为 0 / material id, 0 if not found
	 */
	public int getMeshMaterialId(String meshPath) {
		Integer materialId = materialIdsByPath.get(meshPath);
		if (materialId == null) {
			return 0;
		}
		return materialId;
	}

	/** 返回网格路径 / Returns the mesh paths */
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
