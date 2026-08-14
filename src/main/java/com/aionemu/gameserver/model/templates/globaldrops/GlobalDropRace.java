package com.aionemu.gameserver.model.templates.globaldrops;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;

/**
 * 全局掉落种族模板（静态数据/XML）。
 * Global drop race template (static data/XML).
 *
 * @author Wnkrz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GlobalDropRace")
public class GlobalDropRace {
	@XmlAttribute(name = "race", required = true)
	protected Race race;

	/** 获取种族。 / Returns the race. */
	public Race getRace() {
		return race;
	}
}
