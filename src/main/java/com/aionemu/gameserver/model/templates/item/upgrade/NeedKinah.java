package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Need 基纳模板（静态数据/XML）。
 * XML template.
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "NeedKinah")
@XmlAccessorType(XmlAccessType.FIELD)
public class NeedKinah {
	@XmlAttribute(name = "count")
	private int count;

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}
}
