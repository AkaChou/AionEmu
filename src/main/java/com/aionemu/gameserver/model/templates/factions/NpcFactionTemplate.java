package com.aionemu.gameserver.model.templates.factions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;

/**
 * NPC 势力模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NpcFaction")
public class NpcFactionTemplate {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id", required = true)
	protected int id;

	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name")
	protected String name;

	/** 返回名称 ID / Returns the name id */
	@XmlAttribute(name = "nameId")
	protected int nameId;

	/** 获取分类。 / Returns the category. */
	@XmlAttribute(name = "category")
	protected FactionCategory category;

	@XmlAttribute(name = "minlevel")
	protected Integer minlevel;

	@XmlAttribute(name = "maxlevel")
	protected int maxlevel = 99;

	@XmlAttribute(name = "auto_join")
	protected Integer autoJoin;

	/** 返回 auto quit / Returns the auto quit */
	@XmlAttribute(name = "auto_quit")
	protected int autoQuit = 40;

	/** 获取种族。 / Returns the race. */
	@XmlAttribute(name = "race")
	protected Race race;

	/** 返回 NPC ID / Returns the npc id */
	@XmlAttribute(name = "npcid")
	protected int npcId;

	/** 获取技能点。 / Returns the skill points. */
	@XmlAttribute(name = "skill_points")
	protected int skillPoints;

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return minlevel;
	}

	/** 获取最大等级。 / Returns the max level. */
	public int getMaxLevel() {
		return maxlevel;
	}

	/** 是否导师 / Whether mentor */
	public boolean isMentor() {
		return category == FactionCategory.MENTOR;
	}

	/** 返回自动加入 / Returns the auto join*/
	public int getAutoJoin() {
		return autoJoin;
	}
}
