package com.aionemu.gameserver.model.templates.spawns.landingspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.landing.LandingStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 登陆区域刷新点模板：按登陆状态组织刷怪。
 * Landing zone spawn template: organizes spawns by landing state.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LandingSpawn")
public class LandingSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "landing_level")
	private List<LandingSpawn.LandingStateTemplate> LandingStateTemplate;

	/** 返回登陆状态模板 / Returns the landing state templates */
	public List<LandingStateTemplate> getSiegeModTemplates() {
		return LandingStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "LandingStateTemplate")
	public static class LandingStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 获取登陆类型。 / Returns the landing type.
         */
        @XmlAttribute(name = "level")
		private LandingStateType landingType;

    }
}
