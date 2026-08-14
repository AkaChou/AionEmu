package com.aionemu.gameserver.model.templates.item.upgrade;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 升级材料模板（静态数据/XML）。
 * Upgrade materials template (static data/XML).
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "UpgradeMaterials")
@XmlAccessorType(XmlAccessType.FIELD)
public class UpgradeMaterials {
	@XmlElement(required = true)
	protected List<SubMaterialItem> sub_material_item;

	/** 返回子材料列表 / Returns the sub material items */
	public List<SubMaterialItem> getSubMaterialItem() {
		return sub_material_item;
	}
}
