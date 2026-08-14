package com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;

/**
 * 永恒之塔刷新点模板（静态数据/XML）。
 * Tower of Eternity spawn template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TowerOfEternitySpawn")
public class TowerOfEternitySpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID。 / Returns the id. */
	public int getId() {
		return id;
	}

	@XmlElement(name = "tower_of_eternity_type")
	private List<TowerOfEternitySpawn.TowerOfEternityStateTemplate> TowerOfEternityStateTemplate;

	/** 返回永恒之塔状态模板。 / Returns the tower of eternity state templates. */
	public List<TowerOfEternityStateTemplate> getSiegeModTemplates() {
		return TowerOfEternityStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "TowerOfEternityStateTemplate")
	public static class TowerOfEternityStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "tstate")
		private TowerOfEternityStateType towerOfEternityType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 返回永恒之塔状态类型。 / Returns the tower of eternity type. */
		public TowerOfEternityStateType getTowerOfEternityType() {
			return towerOfEternityType;
		}
	}
}
