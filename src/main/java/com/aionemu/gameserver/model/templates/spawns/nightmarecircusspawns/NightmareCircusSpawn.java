package com.aionemu.gameserver.model.templates.spawns.nightmarecircusspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 梦魇马戏团刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NightmareCircusSpawn")
public class NightmareCircusSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "nightmare_circus_type")
	private List<NightmareCircusSpawn.NightmareCircusStateTemplate> NightmareCircusStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<NightmareCircusStateTemplate> getSiegeModTemplates() {
		return NightmareCircusStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "NightmareCircusStateTemplate")
	public static class NightmareCircusStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "nstate")
		private NightmareCircusStateType nightmareCircusType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取梦魇马戏团类型。 / Returns the nightmare circus type. */
		public NightmareCircusStateType getNightmareCircusType() {
			return nightmareCircusType;
		}
	}
}
