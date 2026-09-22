package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

/**
 * 物品 Req 模板（静态数据/XML）。
 * XML template.
 * @author xTz
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemReq")
public class ItemReq {

	/** 返回物品 ID / Returns the item id */
	@XmlAttribute(name = "item_id")
	protected int itemId;
	/** 获取物品计数。 / Returns the item count. */
	@XmlAttribute(name = "item_count")
	protected int itemCount;
	/** 返回 err item / Returns the err item */
	@XmlAttribute(name = "err_item")
	protected int errItem;
}
