package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Leader 技能 Light 模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LeaderSkillLight")
public class LeaderSkillLight {
	@XmlAttribute(name = "id")
	protected int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}
}
