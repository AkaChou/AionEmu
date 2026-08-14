package com.aionemu.gameserver.model.templates.itemgroups;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 宠物喂食物品组：喂食物品条目与索引。
 * Pet feed item group: feed entries and group index.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FeedItemGroup")
public abstract class FeedItemGroup {

	@XmlAttribute(name = "group", required = true)
	protected ItemGroupIndex index = ItemGroupIndex.NONE;

	@XmlElement(name = "item")
	private List<ItemRaceEntry> items;

	/** 返回索引 / Returns the index*/
	public ItemGroupIndex getIndex() {
		return index;
	}

	/** 获取物品。 / Returns the items. */
	public List<ItemRaceEntry> getItems() {
		if (items == null) {
			items = new ArrayList<ItemRaceEntry>();
		}
		return items;
	}
}
