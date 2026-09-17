package com.aionemu.gameserver.model.templates.portal;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import lombok.Getter;
import lombok.Setter;

/**
 * 副本 Exit 模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@Getter
@Setter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceExit")
public class InstanceExit {

	/** 设置 instance id / Sets the instance id */
	@XmlAttribute(name = "instance_id")
	protected int instanceId;
	/** 返回 exit world / Returns the exit world */
	@XmlAttribute(name = "exit_world")
	protected int exitWorld;
	/** 获取种族。 / Returns the race. */
	@XmlAttribute(name = "race")
	protected Race race = Race.PC_ALL;
	/** 返回 x / Returns the x */
	@XmlAttribute(name = "x")
	protected float x;
	/** 返回 y / Returns the y */
	@XmlAttribute(name = "y")
	protected float y;
	/** 返回 z / Returns the z */
	@XmlAttribute(name = "z")
	protected float z;
	/** 返回 h / Returns the h */
	@XmlAttribute(name = "h")
	protected byte h;

	/** 返回副本 ID / Returns the instance id */
	public Integer getInstanceId() {
		return instanceId;
	}
}
