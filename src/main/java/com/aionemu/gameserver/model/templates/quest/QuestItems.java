package com.aionemu.gameserver.model.templates.quest;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 任务物品模板（静态数据/XML）。
 * XML template.
 *
 * @author MrPoke
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QuestItems")
public class QuestItems {

	@XmlAttribute(name = "item_id")
	protected Integer itemId;
	@XmlAttribute
	protected Integer count;

	/**
	 * 供解组器使用的构造方法（默认数量为 1）。
	 * Constructor used by the unmarshaller (default count is 1).
	 */
	public QuestItems() {
		this.count = 1;
	}

	public QuestItems(int itemId, int count) {
		super();
		this.itemId = itemId;
		this.count = count;
	}

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
	 * 返回物品数量。
	 * Returns the item count.
	 *
	 * @return 数量 / possible object is {@link Integer}
	 */
	public Integer getCount() {
		return count;
	}
}
