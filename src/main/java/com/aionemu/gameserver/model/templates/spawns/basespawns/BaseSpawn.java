package com.aionemu.gameserver.model.templates.spawns.basespawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 基础刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BaseSpawn")
public class BaseSpawn {
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

	/** 返回基础种族模板 / Returns the base race templates*/
	public List<SimpleRaceTemplate> getBaseRaceTemplates() {
		return simpleRaceTemplates;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "BaseRaceTemplate")
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
