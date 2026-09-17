package com.aionemu.gameserver.model.templates.siegelocation;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Leader 技能 Dark 模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LeaderSkillDark")
public class LeaderSkillDark {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id")
	protected int id;
}
