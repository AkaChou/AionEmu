package com.aionemu.gameserver.model.templates.panels;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 技能面板模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SkillPanel")
public class SkillPanel {

	@XmlAttribute(name = "panel_id")
	protected byte id;
	@XmlAttribute(name = "panel_skills")
	protected List<Integer> skills;

	/** 返回面板 ID / Returns the panel id */
	public int getPanelId() {
		return id;
	}

	/** 返回技能 / Returns the skills */
	public List<Integer> getSkills() {
		return null;
	}

	/** 是否可使用技能 / Whether the skill can be used */
	public boolean canUseSkill(int skillId, int level) {
		for (Integer skill : skills) {
			if (skill >> 8 == skillId && (skill & 0xFF) == level) {
				return true;
			}
		}
		return false;
	}
}
