package com.aionemu.gameserver.model.templates.spawns.beritraspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.beritra.BeritraStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 贝里特拉刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BeritraSpawn")
public class BeritraSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "beritra_type")
	private List<BeritraSpawn.BeritraStateTemplate> BeritraStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<BeritraStateTemplate> getSiegeModTemplates() {
		return BeritraStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "BeritraStateTemplate")
	public static class BeritraStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "bstate")
		private BeritraStateType beritraType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取贝里特拉类型。 / Returns the beritra type. */
		public BeritraStateType getBeritraType() {
			return beritraType;
		}
	}
}
