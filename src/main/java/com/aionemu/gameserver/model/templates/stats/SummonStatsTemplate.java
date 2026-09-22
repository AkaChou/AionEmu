package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 召唤物属性模板（静态数据/XML）。
 * XML template.
 * @author ATracer
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "summon_stats_template")
public class SummonStatsTemplate extends StatsTemplate {

	/**
	 * 返回物理防御。
	 * Returns the physical defense.
	 */
	@XmlAttribute(name = "pdefense")
	private int pdefense;
	/**
	 * 返回魔法抵抗。
	 * Returns the magic resistance.
	 */
	@XmlAttribute(name = "mresist")
	private int mresist;
	/**
	 * 返回魔法暴击。
	 * Returns the magic critical.
	 */
	@XmlAttribute(name = "mcrit")
	private int mcrit;
}
