package com.aionemu.gameserver.model.templates.spawns.moltenusspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.moltenus.MoltenusStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 熔岩魔刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MoltenusSpawn")
public class MoltenusSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "moltenus_type")
	private List<MoltenusSpawn.MoltenusStateTemplate> MoltenusStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<MoltenusStateTemplate> getSiegeModTemplates() {
		return MoltenusStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "MoltenusStateTemplate")
	public static class MoltenusStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "mstate")
		private MoltenusStateType moltenusType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取熔岩魔类型。 / Returns the moltenus type. */
		public MoltenusStateType getMoltenusType() {
			return moltenusType;
		}
	}
}
