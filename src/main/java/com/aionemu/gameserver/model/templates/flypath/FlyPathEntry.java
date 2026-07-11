package com.aionemu.gameserver.model.templates.flypath;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 飞行路径条目模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author KID
 */
@XmlRootElement(name = "flypath_location")
@XmlAccessorType(XmlAccessType.NONE)
public class FlyPathEntry {
	@XmlAttribute(name = "id", required = true)
	private short id;
	@XmlAttribute(name = "sx", required = true)
	private float startX;
	@XmlAttribute(name = "sy", required = true)
	private float startY;
	@XmlAttribute(name = "sz", required = true)
	private float startZ;
	@XmlAttribute(name = "sworld", required = true)
	private int sworld;

	@XmlAttribute(name = "ex", required = true)
	private float endX;
	@XmlAttribute(name = "ey", required = true)
	private float endY;
	@XmlAttribute(name = "ez", required = true)
	private float endZ;
	@XmlAttribute(name = "eworld", required = true)
	private int eworld;

	@XmlAttribute(name = "time", required = true)
	private float time;

	/** 返回 ID / Returns the id */
	public short getId() {
		return id;
	}

	/** 返回开始 X / Returns the start x */
	public float getStartX() {
		return startX;
	}

	/** 返回开始 Y / Returns the start y */
	public float getStartY() {
		return startY;
	}

	/** 返回开始 Z / Returns the start z */
	public float getStartZ() {
		return startZ;
	}

	/** 返回结束 X / Returns the end x */
	public float getEndX() {
		return endX;
	}

	/** 返回结束 Y / Returns the end y */
	public float getEndY() {
		return endY;
	}

	/** 返回结束 Z / Returns the end z */
	public float getEndZ() {
		return endZ;
	}

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
