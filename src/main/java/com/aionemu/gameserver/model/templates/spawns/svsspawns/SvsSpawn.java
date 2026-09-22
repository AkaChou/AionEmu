package com.aionemu.gameserver.model.templates.spawns.svsspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.svs.SvsStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 势力战刷新点模板（静态数据/XML）。
 * XML template.
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SvsSpawn")
public class SvsSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "svs_type")
	private List<SvsSpawn.SvsStateTemplate> SvsStateTemplate;

	/** 返回要塞模式模板 / Returns the siege mod templates */
	public List<SvsStateTemplate> getSiegeModTemplates() {
		return SvsStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "SvsStateTemplate")
	public static class SvsStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 获取势力战类型。 / Returns the svs type.
         */
        @XmlAttribute(name = "pstate")
		private SvsStateType svsType;

    }
}
