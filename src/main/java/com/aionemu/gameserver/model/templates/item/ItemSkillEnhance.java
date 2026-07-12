package com.aionemu.gameserver.model.templates.item;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.PlayerClass;

/**
 * 物品技能 Enhance 模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ItemSkillEnhance")
public class ItemSkillEnhance {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "skill_id")
	protected List<Integer> skillId;

	@XmlAttribute(name = "player_class")
	private PlayerClass classId = PlayerClass.ALL;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 返回技能 ID / Returns the skill id */
	public List<Integer> getSkillId() {
		if (skillId == null) {
			skillId = new ArrayList<Integer>();
		}
		return skillId;
	}

	/** 返回职业 ID / Returns the class id */
	public PlayerClass getClassId() {
		return classId;
	}
}
