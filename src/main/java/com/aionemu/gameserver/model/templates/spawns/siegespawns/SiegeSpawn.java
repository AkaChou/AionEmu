package com.aionemu.gameserver.model.templates.spawns.siegespawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 要塞刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SiegeSpawn")
public class SiegeSpawn {

	/**
	 * -- GETTER --
	 * 返回攻城种族模板 / Returns the siege race templates
	 */
	@XmlElement(name = "siege_race")
	private List<SiegeRaceTemplate> siegeRaceTemplates;
	/**
	 * -- GETTER --
	 * 返回攻城 ID / Returns the siege id
	 */
	@XmlAttribute(name = "siege_id")
	private int siegeId;

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "SiegeRaceTemplate")
	public static class SiegeRaceTemplate {

		/**
		 * -- GETTER --
		 * 返回要塞模式模板 / Returns the siege mod templates
		 */
		@Getter
		@XmlElement(name = "siege_mod")
		private List<SiegeModTemplate> SiegeModTemplates;
		@XmlAttribute(name = "race")
		private SiegeRace race;

		/** 获取要塞种族。 / Returns the siege race. */
		public SiegeRace getSiegeRace() {
			return race;
		}

		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "SiegeModTemplate")
		public static class SiegeModTemplate {
            /**
             * -- GETTER --
             * 获取刷新。 / Returns the spawns.
             */
            @Getter
            @XmlElement(name = "spawn")
			private List<Spawn> spawns;
			@XmlAttribute(name = "mod")
			private SiegeModType siegeMod;

            /** 获取要塞模式类型。 / Returns the siege mod type. */
			public SiegeModType getSiegeModType() {
				return siegeMod;
			}
		}
	}
}
