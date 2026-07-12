package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 全局掉落区域模板（静态数据/XML）。
 * XML template.
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropZone")
public class GlobalDropZone {
	@XmlAttribute(name = "zone", required = true)
	protected String zone;

	/** 获取区域。 / Returns the zone. */
	public String getZone() {
		return zone;
	}
}
