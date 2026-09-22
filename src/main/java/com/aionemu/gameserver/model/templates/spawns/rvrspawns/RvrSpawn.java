package com.aionemu.gameserver.model.templates.spawns.rvrspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.rvr.RvrStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 阵营战刷新点模板（静态数据/XML）。
 * XML template.
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RvrSpawn")
public class RvrSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "rvr_type")
	private List<RvrSpawn.RvrStateTemplate> RvrStateTemplate;

	/** 返回阵营战状态模板 / Returns the rvr state templates */
	public List<RvrStateTemplate> getSiegeModTemplates() {
		return RvrStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "RvrStateTemplate")
	public static class RvrStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 获取阵营战类型。 / Returns the rvr type.
         */
        @XmlAttribute(name = "rstate")
		private RvrStateType rvrType;

    }
}
