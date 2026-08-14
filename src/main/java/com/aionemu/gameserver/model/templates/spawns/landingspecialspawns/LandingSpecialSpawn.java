package com.aionemu.gameserver.model.templates.spawns.landingspecialspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 登陆特别刷新点模板（静态数据/XML）。
 * Landing Special Spawn Template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LandingSpecialSpawn")
public class LandingSpecialSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "landing_special_type")
	private List<LandingSpecialSpawn.LandingSpStateTemplate> LandingSpStateTemplate;

	/** 返回登陆特别状态模板列表 / Returns the landing special state templates */
	public List<LandingSpStateTemplate> getSiegeModTemplates() {
		return LandingSpStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "LandingSpStateTemplate")
	public static class LandingSpStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "fstate")
		private LandingSpecialStateType landingSpecialType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 返回登陆特别类型 / Returns the landing special type */
		public LandingSpecialStateType getLandingSpecialType() {
			return landingSpecialType;
		}
	}
}
