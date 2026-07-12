package com.aionemu.gameserver.model.templates.spawns.conquestspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.conquest.ConquestStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 征服刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConquestSpawn")
public class ConquestSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "conquest_type")
	private List<ConquestSpawn.ConquestStateTemplate> ConquestStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<ConquestStateTemplate> getSiegeModTemplates() {
		return ConquestStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "ConquestStateTemplate")
	public static class ConquestStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "ostate")
		private ConquestStateType conquestType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取征服类型。 / Returns the conquest type. */
		public ConquestStateType getConquestType() {
			return conquestType;
		}
	}
}
