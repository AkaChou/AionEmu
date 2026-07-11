package com.aionemu.gameserver.model.templates.item.upgrade;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 升级材料模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "UpgradeMaterials")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradeMaterials {
	@XmlElement(required = true)
	protected List<SubMaterialItem> sub_material_item;

	/** 返回 sub material item / Returns the sub material item */
	public List<SubMaterialItem> getSubMaterialItem() {
		return sub_material_item;
	}
}
