package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 宠物属性模板（静态数据/XML）。
 * Pet stats template (static data / XML).
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

	/** 返回反应动作 / Returns the reaction */
	public String getReaction() {
		return reaction;
	}

	/** 返回奔跑速度 / Returns the run speed */
	public float getRunSpeed() {
		return runSpeed;
	}

	/** 返回行走速度 / Returns the walk speed */
	public float getWalkSpeed() {
		return walkSpeed;
	}

	/** 返回高度 / Returns the height */
	public float getHeight() {
		return height;
	}

	/** 返回飞行高度 / Returns the altitude */
	public float getAltitude() {
		return altitude;
	}
}
