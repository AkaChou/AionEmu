package com.aionemu.gameserver.model.templates.minion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.stats.StatsTemplate;

/**
 * 守护灵属性模板（静态数据/XML）。
 * XML template.
 *
 * @author Falke_34
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MinionStatsTemplate")
public class MinionStatsTemplate extends StatsTemplate {

	@XmlAttribute(name = "run_speed")
	private float runSpeed;
	@XmlAttribute(name = "walk_speed")
	private float walkSpeed;
	@XmlAttribute(name = "altitude")
	private float altitude;

	/** 返回 run speed / Returns the run speed */
	public float getRunSpeed() {
		return runSpeed;
	}

	/** 返回 walk speed / Returns the walk speed */
	public float getWalkSpeed() {
		return walkSpeed;
	}

	/** 返回 altitude / Returns the altitude */
	public float getAltitude() {
		return altitude;
	}
}
