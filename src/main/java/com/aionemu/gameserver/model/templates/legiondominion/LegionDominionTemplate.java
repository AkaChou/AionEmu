package com.aionemu.gameserver.model.templates.legiondominion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 军团领地模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dominion_locations")
public class LegionDominionTemplate {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "world")
	protected int world;

	@XmlAttribute(name = "name")
	protected String nameId;

	/** 返回军团领地 ID / Returns the legion dominion id */
	public int getLegionDominionId() {
		return this.id;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return this.world;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return nameId;
	}
}
