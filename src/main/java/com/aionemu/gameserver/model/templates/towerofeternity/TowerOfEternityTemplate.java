package com.aionemu.gameserver.model.templates.towerofeternity;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;

/**
 * 永恒之塔模板（静态数据/XML）。
 * Tower of Eternity template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Tower")
public class TowerOfEternityTemplate extends TowerOfEternityLocation {
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "world")
	protected int world;

	/** 返回 ID。 / Returns the id. */
	public int getId() {
		return this.id;
	}

	/** 返回世界 ID。 / Returns the world id. */
	public int getWorldId() {
		return this.world;
	}
}
