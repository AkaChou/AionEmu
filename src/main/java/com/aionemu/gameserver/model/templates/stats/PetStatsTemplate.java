package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 宠物属性模板（静态数据/XML）。
 * XML template.
 *
 * @author IlBuono
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "petstats")
public class PetStatsTemplate {

	@XmlAttribute(name = "reaction")
	private String reaction;
	@XmlAttribute(name = "run_speed")
	private float runSpeed;
	@XmlAttribute(name = "walk_speed")
	private float walkSpeed;
	@XmlAttribute(name = "height")
	private float height;
	@XmlAttribute(name = "altitude")
	private float altitude;

	/** 返回 reaction / Returns the reaction */
	public String getReaction() {
		return reaction;
	}

	/** 返回 run speed / Returns the run speed */
	public float getRunSpeed() {
		return runSpeed;
	}

	/** 返回 walk speed / Returns the walk speed */
	public float getWalkSpeed() {
		return walkSpeed;
	}

	/** 返回 height / Returns the height */
	public float getHeight() {
		return height;
	}

	/** 返回 altitude / Returns the altitude */
	public float getAltitude() {
		return altitude;
	}
}
