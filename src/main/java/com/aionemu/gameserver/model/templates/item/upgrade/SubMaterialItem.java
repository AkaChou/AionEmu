package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * 子材料物品模板（静态数据/XML）。
 * Sub material item template (static data/XML).
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "SubMaterialItem")
@XmlAccessorType(XmlAccessType.FIELD)
public class SubMaterialItem {
	/** 返回 ID / Returns the id */
	@Getter
	@Setter
	@XmlAttribute
	private int id;

	/** 获取计数。 / Returns the count. */
	@Getter
	@Setter
	@XmlAttribute
	private int count;
}
