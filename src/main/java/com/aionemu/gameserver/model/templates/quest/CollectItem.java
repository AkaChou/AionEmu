package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Collect 物品模板（静态数据/XML）。
 * XML template. / XML template.
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
	  * 获取 itemId 属性值。
	  * Gets the value of the itemId property
	  * @return possible object is {@link Integer }
	  */
	public Integer getItemId() {
		return itemId;
	}

	/**
	 * 获取 value 的数量 property。 / Gets the value of the count property
	 *
	 * @return possible object is {@link Integer }
	 */
	public Integer getCount() {
		return count;
	}
}
