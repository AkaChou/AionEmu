package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 处理方式模板：物品处置计数。
 * Disposition template: item disposal count.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Disposition")
public class Disposition {
	/** 获取计数。 / Returns the count. */
	@XmlAttribute
	protected int count;

	/** 返回 ID / Returns the id */
	@XmlAttribute
	protected int id;
}
