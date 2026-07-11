package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * 全局掉落物品模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropItem")
public class GlobalDropItem {
	@XmlAttribute(name = "id", required = true)
	protected int itemId;

	@XmlTransient
	private ItemTemplate template;

	/** 返回 ID / Returns the id */
	public int getId() {
		return itemId;
	}

	/** 获取物品模板。 / Returns the item template. */
	public ItemTemplate getItemTemplate() {
		if (template == null) {
			template = DataManager.ITEM_DATA.getItemTemplate(itemId);
		}
		return template;
	}
}
