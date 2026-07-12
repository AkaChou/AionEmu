package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Require 技能模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RequireSkill")
public class RequireSkill {
	@XmlAttribute
	protected List<Integer> skillIds;

	/** 返回技能 ID / Returns the skill ids */
	public List<Integer> getSkillIds() {
		if (skillIds == null) {
			skillIds = new ArrayList<Integer>();
		}
		return this.skillIds;
	}
}
