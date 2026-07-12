package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 宠物属性模板（静态数据/XML）。
 * XML template.
 *
 * @author M@xx
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "petstats")
public class PetStatsTemplate {

	@XmlAttribute(name = "reaction", required = true)
	private String reaction;

	@XmlAttribute(name = "run_speed", required = true)
	private float runSpeed;

	@XmlAttribute(name = "walk_speed", required = true)
	private float walkSpeed;

	@XmlAttribute(name = "height", required = true)
	private float height;

	@XmlAttribute(name = "altitude", required = true)
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
