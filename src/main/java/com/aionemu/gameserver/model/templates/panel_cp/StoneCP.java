package com.aionemu.gameserver.model.templates.panel_cp;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 石创造点模板（静态数据/XML）。
 * Stone CP template (static data / XML).
 */

@Getter
@XmlType(name = "stone_cp")
@XmlAccessorType(XmlAccessType.NONE)
public class StoneCP {
	/** 返回 ID / Returns the id */
	@XmlAttribute
	protected int id;

	/** 获取名称。 / Returns the name. */
	@XmlAttribute
	protected String name;

	@XmlAttribute
	protected int cp;

	/** 获取创造点。 / Returns the cp. */
	public int getCP() {
		return cp;
	}
}
