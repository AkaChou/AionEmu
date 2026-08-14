package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Collect 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CollectItem")
public class CollectItem {

	@XmlAttribute(name = "item_id")
	protected Integer itemId;
	@XmlAttribute
	protected Integer count;

	 /**
	  * 返回物品 ID。
	  * Returns the item id.
	  *
	  * @return 物品 ID / possible object is {@link Integer}
	  */
	public Integer getItemId() {
		return itemId;
	}

	/**
	 * 返回所需数量。
	 * Returns the required count.
	 *
	 * @return 数量 / possible object is {@link Integer}
	 */
	public Integer getCount() {
		return count;
	}
}
