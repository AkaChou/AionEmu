package com.aionemu.gameserver.model.templates.globaldrops;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落物品模板（静态数据/XML）。
 * XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropItems")
public class GlobalDropItems {
	@XmlElement(name = "gd_item")
	protected List<GlobalDropItem> gdItems;

	/** 获取全局掉落物品。 / Returns the global drop items. */
	public List<GlobalDropItem> getGlobalDropItems() {
		if (gdItems == null) {
			gdItems = new ArrayList<GlobalDropItem>();
		}
		return this.gdItems;
	}
}
