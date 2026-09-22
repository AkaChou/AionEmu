package com.aionemu.gameserver.model.templates.spawns.conquestspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.conquest.ConquestStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 征服刷新点模板（静态数据/XML）。
 * Conquest spawn template (static data/XML).
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ConquestSpawn")
public class ConquestSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "conquest_type")
	private List<ConquestSpawn.ConquestStateTemplate> ConquestStateTemplate;

	/** 返回攻城模式模板列表。 / Returns the siege mod templates. */
	public List<ConquestStateTemplate> getSiegeModTemplates() {
		return ConquestStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "ConquestStateTemplate")
	public static class ConquestStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 获取征服类型。 / Returns the conquest type.
         */
        @XmlAttribute(name = "ostate")
		private ConquestStateType conquestType;

    }
}
