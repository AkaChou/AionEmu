package com.aionemu.gameserver.model.templates.item.upgrade;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 需要基纳模板（静态数据/XML）。
 * Need Kinah template (static data/XML).
 *
 * @author Ranastic (Encom)
 */

@Getter
@XmlRootElement(name = "NeedKinah")
@XmlAccessorType(XmlAccessType.FIELD)
public class NeedKinah {
	/** 获取计数。 / Returns the count. */
	@XmlAttribute(name = "count")
	private int count;
}
