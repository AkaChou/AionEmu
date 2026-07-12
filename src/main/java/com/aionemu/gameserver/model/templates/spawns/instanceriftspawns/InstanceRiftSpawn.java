package com.aionemu.gameserver.model.templates.spawns.instanceriftspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.instancerift.InstanceRiftStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 副本裂隙刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstanceRiftSpawn")
public class InstanceRiftSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "instance_rift_type")
	private List<InstanceRiftSpawn.InstanceRiftStateTemplate> InstanceRiftStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<InstanceRiftStateTemplate> getSiegeModTemplates() {
		return InstanceRiftStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "InstanceRiftStateTemplate")
	public static class InstanceRiftStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "estate")
		private InstanceRiftStateType instanceRiftType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取副本裂隙类型。 / Returns the instance rift type. */
		public InstanceRiftStateType getInstanceRiftType() {
			return instanceRiftType;
		}
	}
}
