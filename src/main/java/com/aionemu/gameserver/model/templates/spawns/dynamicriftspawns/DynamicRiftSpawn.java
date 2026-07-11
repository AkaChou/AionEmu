package com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 动态裂隙刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DynamicRiftSpawn")
public class DynamicRiftSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "dynamic_rift_type")
	private List<DynamicRiftSpawn.DynamicRiftStateTemplate> DynamicRiftStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<DynamicRiftStateTemplate> getSiegeModTemplates() {
		return DynamicRiftStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "DynamicRiftStateTemplate")
	public static class DynamicRiftStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "dstate")
		private DynamicRiftStateType dynamicRiftType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取动态裂隙类型。 / Returns the dynamic rift type. */
		public DynamicRiftStateType getDynamicRiftType() {
			return dynamicRiftType;
		}
	}
}
