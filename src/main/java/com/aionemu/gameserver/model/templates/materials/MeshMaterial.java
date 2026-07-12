package com.aionemu.gameserver.model.templates.materials;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Mesh 材料模板（静态数据/XML）。
 * XML template.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeshMaterial")
public class MeshMaterial {

	@XmlAttribute(name = "material_id", required = true)
	protected int materialId;

	@XmlAttribute(name = "path", required = true)
	protected String path;

	@XmlAttribute(name = "zone")
	private String zoneName;

	/** 获取区域名称。 / Returns the zone name. */
	public String getZoneName() {
		return zoneName;
	}
}
