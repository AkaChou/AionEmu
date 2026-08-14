package com.aionemu.gameserver.model.templates.spawns.zorshivdredgionspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.spawns.Spawn;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionStateType;

/**
 * 佐希夫无畏舰刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ZorshivDredgionSpawn")
public class ZorshivDredgionSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "zorshiv_dredgion_type")
	private List<ZorshivDredgionSpawn.ZorshivDredgionStateTemplate> ZorshivDredgionStateTemplate;

	/** 返回要塞模式模板 / Returns the siege mod templates */
	public List<ZorshivDredgionStateTemplate> getSiegeModTemplates() {
		return ZorshivDredgionStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "ZorshivDredgionStateTemplate")
	public static class ZorshivDredgionStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "zstate")
		private ZorshivDredgionStateType zorshivDredgionType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取佐希夫无畏舰类型。 / Returns the zorshiv dredgion type. */
		public ZorshivDredgionStateType getZorshivDredgionType() {
			return zorshivDredgionType;
		}
	}
}
