package com.aionemu.gameserver.model.templates.panel_cp;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 面板创造点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlType(name = "panel_cp")
@XmlAccessorType(XmlAccessType.NONE)
public class PanelCp {
	@XmlAttribute
	protected int id;
	@XmlAttribute
	protected String name;
	@XmlAttribute(name = "panelType", required = true)
	private PanelCpType panelCpType;
	@XmlAttribute
	protected int learnSkill;
	@XmlAttribute
	protected int additionalSkill;
	@XmlAttribute
	protected int statsId;
	@XmlAttribute
	protected int skillId;
	@XmlAttribute
	protected int statValue;
	@XmlAttribute
	protected int cost;
	@XmlAttribute
	protected int countMax;
	@XmlAttribute
	protected int costAdj;
	@XmlAttribute
	protected int preCondId;
	@XmlAttribute
	protected int preEnchantCount;
	@XmlAttribute
	protected int minLevel;

	/** 返回 ID / Returns the id */
	public int getId() {
		return this.id;
	}

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取面板创造点类型。 / Returns the panel cp type. */
	public PanelCpType getPanelCpType() {
		return panelCpType;
	}

	/** 返回 learn skill / Returns the learn skill */
	public int getLearnSkill() {
		return learnSkill;
	}

	/** 返回 additional skill / Returns the additional skill */
	public int getAdditionalSkill() {
		return additionalSkill;
	}

	/** 返回 stats id / Returns the stats id */
	public int getStatsId() {
		return statsId;
	}

	/** 返回技能 ID / Returns the skill id */
	public int getSkillId() {
		return skillId;
	}

	/** 获取属性值。 / Returns the stat value. */
	public int getStatValue() {
		return statValue;
	}

	/** 返回 cost / Returns the cost */
	public int getCost() {
		return cost;
	}

	/** 返回数量最大 / Returns the count max*/
	public int getCountMax() {
		return countMax;
	}

	/** 返回 cost adj / Returns the cost adj */
	public int getCostAdj() {
		return costAdj;
	}

	/** 返回 pre cond id / Returns the pre cond id */
	public int getPreCondId() {
		return preCondId;
	}

	/** 返回 pre enchant count / Returns the pre enchant count */
	public int getPreEnchantCount() {
		return preEnchantCount;
	}

	/** 获取最小等级。 / Returns the min level. */
	public int getMinLevel() {
		return minLevel;
	}
}
