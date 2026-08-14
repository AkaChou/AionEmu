package com.aionemu.gameserver.model.templates.quest;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Collect 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CollectItems", propOrder = { "collectItem" })
public class CollectItems {

	@XmlElement(name = "collect_item")
	protected List<CollectItem> collectItem;

	/**
	 * 返回收集物品列表（惰性初始化，修改会直接反映到 JAXB 对象）。
	 * Returns the collect item list (lazily initialized; modifications are reflected in the JAXB object).
	 *
	 * @return 收集物品列表 / list of {@link CollectItem}
	 */
	public List<CollectItem> getCollectItem() {
		if (collectItem == null) {
			collectItem = new ArrayList<CollectItem>();
		}
		return this.collectItem;
	}
}
