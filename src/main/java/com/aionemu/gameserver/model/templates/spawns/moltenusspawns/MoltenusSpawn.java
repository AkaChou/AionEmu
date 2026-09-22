package com.aionemu.gameserver.model.templates.spawns.moltenusspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.moltenus.MoltenusStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 熔岩魔刷新点模板（静态数据/XML）。
 * Moltenus spawn template (static data/XML).
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MoltenusSpawn")
public class MoltenusSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "moltenus_type")
	private List<MoltenusSpawn.MoltenusStateTemplate> MoltenusStateTemplate;

	/** 返回要塞模式模板 / Returns the siege mod templates */
	public List<MoltenusStateTemplate> getSiegeModTemplates() {
		return MoltenusStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "MoltenusStateTemplate")
	public static class MoltenusStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 获取熔岩魔类型。 / Returns the moltenus type.
         */
        @XmlAttribute(name = "mstate")
		private MoltenusStateType moltenusType;

    }
}
