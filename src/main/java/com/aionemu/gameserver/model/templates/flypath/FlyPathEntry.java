package com.aionemu.gameserver.model.templates.flypath;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

/**
 * 飞行路径条目模板（静态数据/XML）。
 * XML template.
 * @author KID
 */
@Getter
@XmlRootElement(name = "flypath_location")
@XmlAccessorType(XmlAccessType.NONE)
public class FlyPathEntry {
	/** 返回 ID / Returns the id */
	@XmlAttribute(name = "id", required = true)
	private short id;
	/** 返回开始 X / Returns the start x */
	@XmlAttribute(name = "sx", required = true)
	private float startX;
	/** 返回开始 Y / Returns the start y */
	@XmlAttribute(name = "sy", required = true)
	private float startY;
	/** 返回开始 Z / Returns the start z */
	@XmlAttribute(name = "sz", required = true)
	private float startZ;
	@XmlAttribute(name = "sworld", required = true)
	private int sworld;

	/** 返回结束 X / Returns the end x */
	@XmlAttribute(name = "ex", required = true)
	private float endX;
	/** 返回结束 Y / Returns the end y */
	@XmlAttribute(name = "ey", required = true)
	private float endY;
	/** 返回结束 Z / Returns the end z */
	@XmlAttribute(name = "ez", required = true)
	private float endZ;
	@XmlAttribute(name = "eworld", required = true)
	private int eworld;

	@XmlAttribute(name = "time", required = true)
	private float time;

	/** 返回开始世界 ID / Returns the start world id */
	public int getStartWorldId() {
		return sworld;
	}

	/** 返回结束世界 ID / Returns the end world id */
	public int getEndWorldId() {
		return eworld;
	}

	/** 返回 time in ms / Returns the time in ms */
	public int getTimeInMs() {
		return (int) (time * 1000);
	}
}
