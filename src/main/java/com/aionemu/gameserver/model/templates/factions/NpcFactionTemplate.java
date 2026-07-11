package com.aionemu.gameserver.model.templates.factions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * NPC 势力模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NpcFaction")
public class NpcFactionTemplate {
	@XmlAttribute(name = "id", required = true)
	protected int id;

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "nameId")
	protected int nameId;

	@XmlAttribute(name = "category")
	protected FactionCategory category;

	@XmlAttribute(name = "minlevel")
	protected Integer minlevel;

	@XmlAttribute(name = "maxlevel")
	protected int maxlevel = 99;

	@XmlAttribute(name = "auto_join")
	protected Integer autoJoin;

	@XmlAttribute(name = "auto_quit")
	protected int autoQuit = 40;

	@XmlAttribute(name = "race")
	protected Race race;

	@XmlAttribute(name = "npcid")
	protected int npcId;

	@XmlAttribute(name = "skill_points")
	protected int skillPoints;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回名称 ID / Returns the name id */
	public int getNameId() {
		return nameId;
	}

	/** 获取分类。 / Returns the category. */
	public FactionCategory getCategory() {
		return category;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return minlevel;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return maxlevel;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 是否导师 / Whether mentor */
	public boolean isMentor() {
		return category == FactionCategory.MENTOR;
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 获取技能点。 / Returns the skill points. */
	public int getSkillPoints() {
		return skillPoints;
	}

	/** 返回自动加入 / Returns the auto join*/
	public int getAutoJoin() {
		return autoJoin;
	}

	/** 返回 auto quit / Returns the auto quit */
	public int getAutoQuit() {
		return autoQuit;
	}
}
