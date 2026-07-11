package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 物品 Req 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemReq")
public class ItemReq {

	@XmlAttribute(name = "item_id")
	protected int itemId;
	@XmlAttribute(name = "item_count")
	protected int itemCount;
	@XmlAttribute(name = "err_item")
	protected int errItem;

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return itemId;
	}

	/** 设置物品 ID / Sets the item id */
	public void setItemId(int value) {
		this.itemId = value;
	}

	/** 获取物品计数。 / Returns the item count. */
	public int getItemCount() {
		return itemCount;
	}

	/** 设置物品计数。 / Sets the item count. */
	public void setItemCount(int value) {
		this.itemCount = value;
	}

	/** 返回 err item / Returns the err item */
	public int getErrItem() {
		return errItem;
	}
}
