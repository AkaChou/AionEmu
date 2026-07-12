package com.aionemu.gameserver.model.templates.spawns.outpostspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 前哨刷新点模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OutpostSpawn")
public class OutpostSpawn {
	@XmlAttribute(name = "id")
	private int id;

	@XmlAttribute(name = "world")
	private int world;

	@XmlElement(name = "simple_race")
	private List<SimpleRaceTemplate> simpleRaceTemplates;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return world;
	}

	/** 返回 outpost race templates / Returns the outpost race templates */
	public List<SimpleRaceTemplate> getOutpostRaceTemplates() {
		return simpleRaceTemplates;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "OutpostRaceTemplate")
	public static class SimpleRaceTemplate {
		@XmlAttribute(name = "race")
		private Race race;

		/** 获取基础种族。 / Returns the base race. */
		public Race getBaseRace() {
			return race;
		}

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}
	}
}
