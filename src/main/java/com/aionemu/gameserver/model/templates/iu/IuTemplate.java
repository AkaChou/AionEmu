package com.aionemu.gameserver.model.templates.iu;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * IU 活动模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Iu")
public class IuTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
