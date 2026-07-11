package com.aionemu.gameserver.model.templates.minion;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 守护灵 Actions 模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Falke_34
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MinionActions")
public class MinionActions {

	@XmlElement(name = "skill")
	protected ArrayList<MinionSkill> skillActions;

	/** 返回 skills collections / Returns the skills collections */
	public Collection<MinionSkill> getSkillsCollections() {
		return skillActions != null ? skillActions : null;
	}
}
