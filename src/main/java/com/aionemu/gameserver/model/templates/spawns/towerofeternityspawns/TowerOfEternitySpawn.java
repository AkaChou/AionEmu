package com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityStateType;
import lombok.Getter;

/**
 * 永恒之塔刷新点模板（静态数据/XML）。
 * Tower of Eternity spawn template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TowerOfEternitySpawn")
public class TowerOfEternitySpawn {
	/**
	 * -- GETTER --
	 * 返回 ID。 / Returns the id.
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "tower_of_eternity_type")
	private List<TowerOfEternitySpawn.TowerOfEternityStateTemplate> TowerOfEternityStateTemplate;

	/** 返回永恒之塔状态模板。 / Returns the tower of eternity state templates. */
	public List<TowerOfEternityStateTemplate> getSiegeModTemplates() {
		return TowerOfEternityStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "TowerOfEternityStateTemplate")
	public static class TowerOfEternityStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 返回永恒之塔状态类型。 / Returns the tower of eternity type.
         */
        @XmlAttribute(name = "tstate")
		private TowerOfEternityStateType towerOfEternityType;

    }
}
