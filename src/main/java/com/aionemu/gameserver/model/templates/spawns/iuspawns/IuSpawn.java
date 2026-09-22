package com.aionemu.gameserver.model.templates.spawns.iuspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.iu.IuStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * IU 活动刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IuSpawn")
public class IuSpawn {
	/**
	 * -- GETTER --
	 * 返回 ID / Returns the id
	 */
	@Getter
	@XmlAttribute(name = "id")
	private int id;

	@XmlElement(name = "iu_type")
	private List<IuSpawn.IuStateTemplate> IuStateTemplate;

	/** 返回 IU 状态模板列表 / Returns the IU state templates */
	public List<IuStateTemplate> getSiegeModTemplates() {
		return IuStateTemplate;
	}

	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "IuStateTemplate")
	public static class IuStateTemplate {

        /**
         * -- GETTER --
         * 获取刷新。 / Returns the spawns.
         */
        @XmlElement(name = "spawn")
		private List<Spawn> spawns;

        /**
         * -- GETTER --
         * 返回 IU 状态类型 / Returns the iu type
         */
        @XmlAttribute(name = "iustate")
		private IuStateType iuType;

    }
}
