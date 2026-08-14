package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 所需技能模板：使用物品需要的技能列表。
 * Required skill template: skills needed to use the item.
 *
 * @author Rinzler
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RequireSkill")
public class RequireSkill {
	@XmlAttribute
	protected List<Integer> skillIds;

	/** 返回技能 ID 列表 / Returns the skill ids */
	public List<Integer> getSkillIds() {
		if (skillIds == null) {
			skillIds = new ArrayList<Integer>();
		}
		return this.skillIds;
	}
}
