package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Need 欧比斯点模板（静态数据/XML）。
 * XML template.
 *
 * @author Ranastic (Encom)
 */

@XmlRootElement(name = "NeedAbyssPoint")
@XmlAccessorType(XmlAccessType.FIELD)
public class NeedAbyssPoint {
	@XmlAttribute(name = "count")
	private int count;

	/** 获取计数。 / Returns the count. */
	public int getCount() {
		return count;
	}
}
