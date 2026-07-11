package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 召唤物属性模板（静态数据/XML）。
 * XML template. / XML template.
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
	 * @return the pdefense
	 */
	public int getPdefense() {
		return pdefense;
	}

	/**
	 * @return the mresist
	 */
	public int getMresist() {
		return mresist;
	}

	/**
	 * @return the mcrit
	 */
	public int getMcrit() {
		return mcrit;
	}
}
