package com.aionemu.gameserver.model.templates.nightmarecircus;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 梦魇马戏团模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "nightmare_circus")
public class NightmareCircusTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
