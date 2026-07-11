package com.aionemu.gameserver.model.templates.spawns.rvrspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.rvr.RvrStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 阵营战刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RvrSpawn")
public class RvrSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "rvr_type")
	private List<RvrSpawn.RvrStateTemplate> RvrStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<RvrStateTemplate> getSiegeModTemplates() {
		return RvrStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "RvrStateTemplate")
	public static class RvrStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "rstate")
		private RvrStateType rvrType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取阵营战类型。 / Returns the rvr type. */
		public RvrStateType getRvrType() {
			return rvrType;
		}
	}
}
