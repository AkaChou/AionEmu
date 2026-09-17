package com.aionemu.gameserver.model.templates.panel_cp;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 面板创造点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@Getter
@XmlType(name = "panel_cp")
@XmlAccessorType(XmlAccessType.NONE)
public class PanelCp {
	/** 返回 ID / Returns the id */
	@XmlAttribute
	protected int id;
	/** 获取名称。 / Returns the name. */
	@XmlAttribute
	protected String name;
	/** 获取面板创造点类型。 / Returns the panel cp type. */
	@XmlAttribute(name = "panelType", required = true)
	private PanelCpType panelCpType;
	/** 返回学习技能 / Returns the learn skill */
	@XmlAttribute
	protected int learnSkill;
	/** 返回附加技能 / Returns the additional skill */
	@XmlAttribute
	protected int additionalSkill;
	/** 返回属性 ID / Returns the stats id */
	@XmlAttribute
	protected int statsId;
	/** 返回技能 ID / Returns the skill id */
	@XmlAttribute
	protected int skillId;
	/** 获取属性值。 / Returns the stat value. */
	@XmlAttribute
	protected int statValue;
	/** 返回消耗 / Returns the cost */
	@XmlAttribute
	protected int cost;
	/** 返回最大数量 / Returns the max count. */
	@XmlAttribute
	protected int countMax;
	/** 返回消耗调整 / Returns the cost adj */
	@XmlAttribute
	protected int costAdj;
	/** 返回前置条件 ID / Returns the pre cond id */
	@XmlAttribute
	protected int preCondId;
	/** 返回前置强化次数 / Returns the pre enchant count */
	@XmlAttribute
	protected int preEnchantCount;
	/** 获取最小等级。 / Returns the min level. */
	@XmlAttribute
	protected int minLevel;
}
