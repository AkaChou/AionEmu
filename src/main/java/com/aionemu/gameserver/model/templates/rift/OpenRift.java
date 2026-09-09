package com.aionemu.gameserver.model.templates.rift;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * Open 裂隙模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OpenRift")
public class OpenRift {
	/** 裂隙时间表 / Rift schedule */
	@Getter
	@XmlAttribute(name = "schedule")
	protected String schedule;

	/** 是否刷出守卫 / Whether to spawn guards */
	@XmlAttribute(name = "spawn")
	protected boolean guards;

	/** 是否刷出守卫 / Whether to spawn guards */
	public boolean spawnGuards() {
		return guards;
	}
}
