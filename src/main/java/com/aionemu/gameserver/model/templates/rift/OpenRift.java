package com.aionemu.gameserver.model.templates.rift;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Open 裂隙模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpenRift")
public class OpenRift {
	@XmlAttribute(name = "schedule")
	protected String schedule;

	@XmlAttribute(name = "spawn")
	protected boolean guards;

	/** 返回 schedule / Returns the schedule */
	public String getSchedule() {
		return schedule;
	}

	/** Spawn guards / Spawn guards */
	public boolean spawnGuards() {
		return guards;
	}
}
