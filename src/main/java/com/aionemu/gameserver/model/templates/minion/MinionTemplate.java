package com.aionemu.gameserver.model.templates.minion;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.stats.calc.functions.StatFunction;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.stats.ModifiersTemplate;

/**
 * 守护灵模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Falke_34
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "MinionTemplate")
public class MinionTemplate {

	@XmlAttribute(name = "id", required = true)
	private int id;

	@XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "nameid")
	private int name_id;

	@XmlAttribute(name = "grade")
	private String grade;

	@XmlAttribute(name = "level")
	private int level;

	@XmlAttribute(name = "growthPoints")
	private int growthPoints;

	@XmlAttribute(name = "growthMax")
	private int growthMax;

	@XmlAttribute(name = "growthCost")
	private int growthCost;

	@XmlElement(name = "modifiers", required = false)
	private ModifiersTemplate modifiers;

	@XmlElement(name = "actions")
	private MinionActions actions;

	@XmlElement(name = "minionstats")
	private MinionStatsTemplate statsTemplate;

	@XmlElement(name = "bound")
	private BoundRadius bound;

	@XmlElement(name = "evolved")
	private MinionEvolved evolved;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 返回 grade / Returns the grade */
	public String getGrade() {
		return this.grade;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return this.level;
	}

	/** 返回 growth pt / Returns the growth pt */
	public int getGrowthPt() {
		return growthPoints;
	}

	/** 返回 max growth value / Returns the max growth value */
	public int getMaxGrowthValue() {
		return growthMax;
	}

	/** 返回 growth cost / Returns the growth cost */
	public int getGrowthCost() {
		return growthCost;
	}

	/** 获取修正器。 / Returns the modifiers. */
	public List<StatFunction> getModifiers() {
		if (this.modifiers != null) {
			return this.modifiers.getModifiers();
		}
		return null;
	}

	/** 获取动作。 / Returns the action. */
	public MinionActions getAction() {
		return this.actions;
	}

	/** 获取属性模板。 / Returns the stats template. */
	public MinionStatsTemplate getStatsTemplate() {
		return this.statsTemplate;
	}

	/** 获取边界半径。 / Returns the bound radius. */
	public BoundRadius getBoundRadius() {
		return this.bound;
	}

	/** 返回 evolved / Returns the evolved */
	public MinionEvolved getEvolved() {
		return this.evolved;
	}
}
