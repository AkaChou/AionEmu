package com.aionemu.gameserver.model.templates.spawns.landingspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 登陆刷新点模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LandingSpawn")
public class LandingSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "landing_level")
	private List<LandingSpawn.LandingStateTemplate> LandingStateTemplate;

	/** 返回 siege mod templates / Returns the siege mod templates */
	public List<LandingStateTemplate> getSiegeModTemplates() {
		return LandingStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "LandingStateTemplate")
	public static class LandingStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "level")
		private LandingStateType landingType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取登陆类型。 / Returns the landing type. */
		public LandingStateType getLandingType() {
			return landingType;
		}
	}
}
