package com.aionemu.gameserver.model.templates.stats;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 宠物属性模板（静态数据/XML）。
 * XML template.
 * @author IlBuono
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "petstats")
public class PetStatsTemplate {

	/** 返回 reaction / Returns the reaction */
	@XmlAttribute(name = "reaction")
	private String reaction;
	/** 返回 run speed / Returns the run speed */
	@XmlAttribute(name = "run_speed")
	private float runSpeed;
	/** 返回 walk speed / Returns the walk speed */
	@XmlAttribute(name = "walk_speed")
	private float walkSpeed;
	/** 返回 height / Returns the height */
	@XmlAttribute(name = "height")
	private float height;
	/** 返回 altitude / Returns the altitude */
	@XmlAttribute(name = "altitude")
	private float altitude;
}
