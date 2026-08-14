package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 子材料物品模板（静态数据/XML）。
 * Sub material item template (static data/XML).
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "SubMaterialItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class SubMaterialItem {
	@XmlAttribute
	private int id;

	@XmlAttribute
	private int count;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 设置 id / Sets the id */
	public void setId(int id) {
		this.id = id;
	}

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}

	/** 设置计数。 / Sets the count. */
	public void setCount(int count) {
		this.count = count;
	}
}
