package com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.dynamicrift.DynamicRiftStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 动态裂隙刷新点模板（静态数据/XML）。
 * Dynamic rift spawn template (static data/XML).
 * @author Rinzler (Encom)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DynamicRiftSpawn")
public class DynamicRiftSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "dynamic_rift_type")
	private List<DynamicRiftSpawn.DynamicRiftStateTemplate> DynamicRiftStateTemplate;

	/** 返回要塞模式模板 / Returns the siege mod templates */
	public List<DynamicRiftStateTemplate> getSiegeModTemplates() {
		return DynamicRiftStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "DynamicRiftStateTemplate")
	public static class DynamicRiftStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 获取动态裂隙类型。 / Returns the dynamic rift type.
         */
        @XmlAttribute(name = "dstate")
		private DynamicRiftStateType dynamicRiftType;

    }
}
