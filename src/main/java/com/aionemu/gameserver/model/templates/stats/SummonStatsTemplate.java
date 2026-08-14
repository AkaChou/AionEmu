package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 召唤物属性模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "summon_stats_template")
public class SummonStatsTemplate extends StatsTemplate {

	@XmlAttribute(name = "pdefense")
	private int pdefense;
	@XmlAttribute(name = "mresist")
	private int mresist;
	@XmlAttribute(name = "mcrit")
	private int mcrit;

	/**
	 * 返回物理防御。
	 * Returns the physical defense.
	 *
	 * @return 物理防御 / the physical defense
	 */
	public int getPdefense() {
		return pdefense;
	}

	/**
	 * 返回魔法抵抗。
	 * Returns the magic resistance.
	 *
	 * @return 魔法抵抗 / the magic resistance
	 */
	public int getMresist() {
		return mresist;
	}

	/**
	 * 返回魔法暴击。
	 * Returns the magic critical.
	 *
	 * @return 魔法暴击 / the magic critical
	 */
	public int getMcrit() {
		return mcrit;
	}
}
