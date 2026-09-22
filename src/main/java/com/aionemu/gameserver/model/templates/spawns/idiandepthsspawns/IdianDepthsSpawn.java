package com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.idiandepths.IdianDepthsStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 伊迪安深渊刷新点模板（静态数据/XML）。
 * Idian Depths spawn XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdianDepthsSpawn")
public class IdianDepthsSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "idian_depths_type")
	private List<IdianDepthsSpawn.IdianDepthsStateTemplate> IdianDepthsStateTemplate;

	/** 返回要塞模式模板 / Returns the siege mod templates */
	public List<IdianDepthsStateTemplate> getSiegeModTemplates() {
		return IdianDepthsStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "IdianDepthsStateTemplate")
	public static class IdianDepthsStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 获取伊迪安深渊类型。 / Returns the idian depths type.
         */
        @XmlAttribute(name = "istate")
		private IdianDepthsStateType idianDepthsType;

    }
}
