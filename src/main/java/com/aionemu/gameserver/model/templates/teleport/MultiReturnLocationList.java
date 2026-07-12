package com.aionemu.gameserver.model.templates.teleport;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * MultiReturn 位置列表模板（静态数据/XML）。
 * XML template.
 */

@XmlType(name = "MultiReturnLocationList")
public class MultiReturnLocationList {
	@XmlAttribute(name = "world_id")
	protected int worldId;

	@XmlAttribute(name = "desc")
	protected String desc;

	/** 返回世界 ID / Returns the world id */
	public final int getWorldId() {
		return worldId;
	}

	/** 返回 desc / Returns the desc */
	public final String getDesc() {
		return desc;
	}
}
