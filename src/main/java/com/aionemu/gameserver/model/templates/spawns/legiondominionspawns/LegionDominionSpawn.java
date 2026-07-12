package com.aionemu.gameserver.model.templates.spawns.legiondominionspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.legiondominion.LegionDominionModType;
import com.aionemu.gameserver.model.legiondominion.LegionDominionRace;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 军团领地刷新点模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LegionDominionSpawn")
public class LegionDominionSpawn {
	@XmlElement(name = "legion_dominion_race")
	private List<LegionDominionRaceTemplate> legionDominionRaceTemplates;

	@XmlAttribute(name = "legion_id")
	private int legionDominionId;

	/** 返回军团领地 ID / Returns the legion dominion id */
	public int getLegionDominionId() {
		return legionDominionId;
	}

	/** 返回 legion dominion race templates / Returns the legion dominion race templates */
	public List<LegionDominionRaceTemplate> getLegionDominionRaceTemplates() {
		return legionDominionRaceTemplates;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "LegionDominionRaceTemplate")
	public static class LegionDominionRaceTemplate {

		@XmlElement(name = "legion_mod")
		private List<LegionDominionModTemplate> LegionDominionModTemplates;

		@XmlAttribute(name = "race")
		private LegionDominionRace race;

		/** 获取军团领地种族。 / Returns the legion dominion race. */
		public LegionDominionRace getLegionDominionRace() {
			return race;
		}

		/** 返回 legion dominion mod templates / Returns the legion dominion mod templates */
		public List<LegionDominionModTemplate> getLegionDominionModTemplates() {
			return LegionDominionModTemplates;
		}

		@XmlAccessorType(XmlAccessType.FIELD)
		@XmlType(name = "LegionDominionModTemplate")
		public static class LegionDominionModTemplate {
			@XmlElement(name = "spawn")
			private List<Spawn> spawns;

			@XmlAttribute(name = "mod")
			private LegionDominionModType legionDominionMod;

			/** 获取刷新。 / Returns the spawns. */
			public List<Spawn> getSpawns() {
				return spawns;
			}

			/** 返回 legion dominion mod type / Returns the legion dominion mod type */
			public LegionDominionModType getLegionDominionModType() {
				return legionDominionMod;
			}
		}
	}
}
