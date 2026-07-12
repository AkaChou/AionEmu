package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 副本 Exit 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceExit")
public class InstanceExit {

	@XmlAttribute(name = "instance_id")
	protected int instanceId;
	@XmlAttribute(name = "exit_world")
	protected int exitWorld;
	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;
	@XmlAttribute(name = "x")
	protected float x;
	@XmlAttribute(name = "y")
	protected float y;
	@XmlAttribute(name = "z")
	protected float z;
	@XmlAttribute(name = "h")
	protected byte h;

	/** 返回副本 ID / Returns the instance id */
	public Integer getInstanceId() {
		return instanceId;
	}

	/** 设置 instance id / Sets the instance id */
	public void setInstanceId(int value) {
		this.instanceId = value;
	}

	/** 返回 exit world / Returns the exit world */
	public int getExitWorld() {
		return exitWorld;
	}

	/** 设置 exit world / Sets the exit world */
	public void setExitWorld(int value) {
		this.exitWorld = value;
	}

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}

	/** 设置种族。 / Sets the race. */
	public void setRace(Race value) {
		this.race = value;
	}

	/** 返回 x / Returns the x */
	public float getX() {
		return x;
	}

	/** 设置 x / Sets the x */
	public void setX(float value) {
		this.x = value;
	}

	/** 返回 y / Returns the y */
	public float getY() {
		return y;
	}

	/** 设置 y / Sets the y */
	public void setY(float value) {
		this.y = value;
	}

	/** 返回 z / Returns the z */
	public float getZ() {
		return z;
	}

	/** 设置 z / Sets the z */
	public void setZ(float value) {
		this.z = value;
	}

	/** 返回 h / Returns the h */
	public byte getH() {
		return h;
	}

	/** 设置 h / Sets the h */
	public void setH(byte value) {
		this.h = value;
	}
}
