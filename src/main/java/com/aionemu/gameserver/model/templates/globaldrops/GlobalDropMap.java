package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 全局掉落地图模板（静态数据/XML）。
 * Global drop map template (static data/XML).
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropMap")
public class GlobalDropMap {
	/** 返回地图 ID。 / Returns the map id. */
	@Getter
	@XmlAttribute(name = "map_id", required = true)
	protected int mapId;
}
