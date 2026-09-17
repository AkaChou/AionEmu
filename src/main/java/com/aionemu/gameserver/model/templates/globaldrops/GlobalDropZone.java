package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 全局掉落区域模板（静态数据/XML）。
 * Global drop zone template (static data/XML).
 *
 * @author Wnkrz
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropZone")
public class GlobalDropZone {
	/** 获取区域。 / Returns the zone. */
	@XmlAttribute(name = "zone", required = true)
	protected String zone;
}
