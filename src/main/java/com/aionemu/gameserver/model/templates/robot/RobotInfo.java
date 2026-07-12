package com.aionemu.gameserver.model.templates.robot;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.RobotBound;

/**
 * Robot 信息模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RobotInfo", propOrder = { "robotbound" })
public class RobotInfo {
	protected RobotBound robotbound;

	@XmlAttribute
	protected Integer type;

	@XmlAttribute(required = true)
	protected int id;

	/** 返回 robot id / Returns the robot id */
	public int getRobotId() {
		return id;
	}

	/** 返回 robot bound / Returns the robot bound */
	public RobotBound getRobotBound() {
		return robotbound;
	}
}
