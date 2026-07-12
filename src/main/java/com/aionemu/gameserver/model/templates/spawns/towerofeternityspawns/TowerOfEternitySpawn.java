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
 * 高塔 Of 永恒刷新点模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TowerOfEternitySpawn")
public class TowerOfEternitySpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "tower_of_eternity_type")
	private List<TowerOfEternitySpawn.TowerOfEternityStateTemplate> TowerOfEternityStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
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

		/** 返回 tower of eternity type / Returns the tower of eternity type */
		public TowerOfEternityStateType getTowerOfEternityType() {
			return towerOfEternityType;
		}
	}
}
