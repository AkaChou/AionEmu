package com.aionemu.gameserver.model.templates.gather;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Ex 材料模板（静态数据/XML）。
 * XML template.
 *
 * @author KID
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Exmaterials", propOrder = { "material" })
public class ExMaterials {

	protected List<Material> material;

	/** 获取材料。 / Returns the material. */
	public List<Material> getMaterial() {
		if (material == null) {
			material = new ArrayList<>();
		}
		return this.material;
	}
}
