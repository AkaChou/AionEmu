package com.aionemu.gameserver.model.templates.spawns.vortexspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.vortex.VortexStateType;

/**
 * 漩涡刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VortexSpawn")
public class VortexSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "state_type")
	private List<VortexSpawn.VortexStateTemplate> VortexStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<VortexStateTemplate> getSiegeModTemplates() {
		return VortexStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "VortexStateTemplate")
	public static class VortexStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "state")
		private VortexStateType stateType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取状态类型。 / Returns the state type. */
		public VortexStateType getStateType() {
			return stateType;
		}
	}
}
