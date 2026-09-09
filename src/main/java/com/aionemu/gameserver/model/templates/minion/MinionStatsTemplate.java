package com.aionemu.gameserver.model.templates.minion;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.model.templates.stats.StatsTemplate;
import lombok.Getter;

/**
 * 守护灵属性模板（静态数据/XML）。
 * Minion stats template (static data/XML).
 *
 * @author Falke_34
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MinionStatsTemplate")
public class MinionStatsTemplate extends StatsTemplate {

	/** 返回 run speed / Returns the run speed */
	@Getter
	@XmlAttribute(name = "run_speed")
	private float runSpeed;
	/** 返回 walk speed / Returns the walk speed */
	@Getter
	@XmlAttribute(name = "walk_speed")
	private float walkSpeed;
	/** 返回 altitude / Returns the altitude */
	@Getter
	@XmlAttribute(name = "altitude")
	private float altitude;
}
