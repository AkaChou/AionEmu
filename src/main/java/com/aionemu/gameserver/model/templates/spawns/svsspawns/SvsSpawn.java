package com.aionemu.gameserver.model.templates.spawns.svsspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.svs.SvsStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 势力战刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SvsSpawn")
public class SvsSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "svs_type")
	private List<SvsSpawn.SvsStateTemplate> SvsStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<SvsStateTemplate> getSiegeModTemplates() {
		return SvsStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "SvsStateTemplate")
	public static class SvsStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "pstate")
		private SvsStateType svsType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取势力战类型。 / Returns the svs type. */
		public SvsStateType getSvsType() {
			return svsType;
		}
	}
}
