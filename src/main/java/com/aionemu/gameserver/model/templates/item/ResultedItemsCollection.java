package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 产物物品集合模板：普通产物、随机产物与产物组。
 * Resulted items collection template: regular, random and set outputs.
 *
 * @author antness
 */
@XmlType(name = "ResultedItemsCollection")
public class ResultedItemsCollection {

	@XmlElement(name = "item")
	protected ArrayList<ResultedItem> items;
	@XmlElement(name = "random_item")
	protected ArrayList<RandomItem> randomItems;
	@XmlElement(name = "item_set")
	protected ArrayList<ResultedItemSet> item_set;

	/** 获取物品。 / Returns the items. */
	public Collection<ResultedItem> getItems() {
		return items != null ? items : Collections.<ResultedItem>emptyList();
	}

	/** 获取套装。 / Returns the item set. */
	public Collection<ResultedItemSet> getItemSet() {
		return item_set != null ? item_set : Collections.<ResultedItemSet>emptyList();
	}

	/** 返回随机物品列表 / Returns the random items */
	public List<RandomItem> getRandomItems() {
		if (randomItems != null) {
			return randomItems;
		} else {
			return new ArrayList<RandomItem>();
		}
	}
}
