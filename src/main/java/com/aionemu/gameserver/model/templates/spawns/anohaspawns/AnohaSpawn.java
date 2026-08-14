package com.aionemu.gameserver.model.templates.spawns.anohaspawns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.anoha.AnohaStateType;
import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 阿诺哈刷新点模板（静态数据/XML）。
 * Anoha spawn template (static data/XML).
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AnohaSpawn")
public class AnohaSpawn {
	@XmlAttribute(name = "id")
	private int id;

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	@XmlElement(name = "anoha_type")
	private List<AnohaSpawn.AnohaStateTemplate> AnohaStateTemplate;

	/** 返回要塞模式模板 / Returns the siege mod templates */
	public List<AnohaStateTemplate> getSiegeModTemplates() {
		return AnohaStateTemplate;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "AnohaStateTemplate")
	public static class AnohaStateTemplate {

		@XmlElement(name = "spawn")
		private List<Spawn> spawns;

		@XmlAttribute(name = "cstate")
		private AnohaStateType anohaType;

		/** 获取刷新。 / Returns the spawns. */
		public List<Spawn> getSpawns() {
			return spawns;
		}

		/** 获取阿诺哈类型。 / Returns the anoha type. */
		public AnohaStateType getAnohaType() {
			return anohaType;
		}
	}
}
