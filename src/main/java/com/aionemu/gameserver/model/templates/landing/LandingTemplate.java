package com.aionemu.gameserver.model.templates.landing;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * 登陆模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "landing")
public class LandingTemplate {
	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "name")
	protected String nameId;

	/** 获取种族。 / Returns the race. */
	@Getter
	@XmlAttribute(name = "race")
	protected Race race;

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return nameId;
	}
}
