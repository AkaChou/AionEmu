package com.aionemu.gameserver.model.templates.towerofeternity;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;
import lombok.Getter;

/**
 * 永恒之塔模板（静态数据/XML）。
 * Tower of Eternity template (static data/XML).
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Tower")
public class TowerOfEternityTemplate extends TowerOfEternityLocation {
	/** 返回 ID。 / Returns the id. */
	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "world")
	protected int world;

	/** 返回世界 ID。 / Returns the world id. */
	public int getWorldId() {
		return this.world;
	}
}
