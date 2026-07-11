package com.aionemu.gameserver.model.templates.landing_special;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 登陆 Special 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "landing_special")
public class LandingSpecialTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "name")
	protected String nameId;

	@XmlAttribute(name = "race")
	protected Race race;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return nameId;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}
}
