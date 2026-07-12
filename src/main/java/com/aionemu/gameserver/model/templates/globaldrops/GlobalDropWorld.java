package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.world.WorldDropType;

/**
 * 全局掉落世界模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropWorld")
public class GlobalDropWorld {
	@XmlAttribute(name = "wd_type", required = true)
	protected WorldDropType wdType;

	/** 获取世界掉落类型。 / Returns the world drop type. */
	public WorldDropType getWorldDropType() {
		return wdType;
	}
}
