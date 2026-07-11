package com.aionemu.gameserver.skillengine.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 动作时间配置：名称、播放速度与是否瞬时技能。
 * Motion timing config: name, playback speed and instant-skill flag.
 *
 * @author kecimis
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Motion")
public class Motion {

	@XmlAttribute(required = true)
	protected String name;

	@XmlAttribute
	protected int speed = 100;

	@XmlAttribute(name = "instant_skill")
	protected boolean instantSkill = false;

	/**
	 * 获取动作名称。
	 * Gets motion name.
	 *
	 * name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 获取播放速度（百分比）。
	 * Gets playback speed (percent).
	 *
	 * speed
	 */
	public int getSpeed() {
		return this.speed;
	}

	/**
	 * 是否瞬时技能（无前摇）。
	 * Whether this is an instant skill (no cast wind-up).
	 *
	 * whether instant
	 */
	public boolean getInstantSkill() {
		return this.instantSkill;
	}
}
