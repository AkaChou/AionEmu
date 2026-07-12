package com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 伊迪安深渊刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdianDepthsSpawn")
public class IdianDepthsSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "idian_depths_type")
	private List<IdianDepthsSpawn.IdianDepthsStateTemplate> IdianDepthsStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<IdianDepthsStateTemplate> getSiegeModTemplates() {
		return IdianDepthsStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "IdianDepthsStateTemplate")
	public static class IdianDepthsStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "istate")
		private IdianDepthsStateType idianDepthsType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取伊迪安深渊类型。 / Returns the idian depths type. */
		public IdianDepthsStateType getIdianDepthsType() {
			return idianDepthsType;
		}
	}
}
