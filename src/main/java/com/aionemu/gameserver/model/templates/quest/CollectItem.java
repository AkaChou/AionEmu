package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Collect 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CollectItem")
public class CollectItem {

	/**
	 * 返回物品 ID。
	 * Returns the item id.
	 *
	 * @return 物品 ID / possible object is {@link Integer}
	 */
	@XmlAttribute(name = "item_id")
	protected Integer itemId;
	/**
	 * 返回所需数量。
	 * Returns the required count.
	 *
	 * @return 数量 / possible object is {@link Integer}
	 */
	@XmlAttribute
	protected Integer count;
}
