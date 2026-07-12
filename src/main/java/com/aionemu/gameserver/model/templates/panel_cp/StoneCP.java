package com.aionemu.gameserver.model.templates.panel_cp;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Stone 创造点模板（静态数据/XML）。
 * XML template.
 */

@XmlType(name = "stone_cp")
@XmlAccessorType(XmlAccessType.NONE)
public class StoneCP {
	@XmlAttribute
	protected int id;

	@XmlAttribute
	protected String name;

	@XmlAttribute
	protected int cp;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取创造点。 / Returns the cp. */
	public int getCP() {
		return cp;
	}
}
