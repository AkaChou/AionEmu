package com.aionemu.gameserver.model.templates.gather;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 材料模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Materials", propOrder = { "material" })
public class Materials {

	protected List<Material> material;

	 /**
	  * 获取 material 属性值。
	  * Gets the value of the material property
	  */
	public List<Material> getMaterial() {
		if (material == null) {
			material = new ArrayList<Material>();
		}
		return this.material;
	}
}
