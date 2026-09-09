package com.aionemu.gameserver.model.templates.pet;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 宠物属性模板（静态数据/XML）。
 * Pet stats template (static data / XML).
 *
 * @author M@xx
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "petstats")
public class PetStatsTemplate {

	/** 返回反应动作 / Returns the reaction */
	@Getter
	@XmlAttribute(name = "reaction", required = true)
	private String reaction;

	/** 返回奔跑速度 / Returns the run speed */
	@Getter
	@XmlAttribute(name = "run_speed", required = true)
	private float runSpeed;

	/** 返回行走速度 / Returns the walk speed */
	@Getter
	@XmlAttribute(name = "walk_speed", required = true)
	private float walkSpeed;

	/** 返回高度 / Returns the height */
	@Getter
	@XmlAttribute(name = "height", required = true)
	private float height;

	/** 返回飞行高度 / Returns the altitude */
	@Getter
	@XmlAttribute(name = "altitude", required = true)
	private float altitude;
}
