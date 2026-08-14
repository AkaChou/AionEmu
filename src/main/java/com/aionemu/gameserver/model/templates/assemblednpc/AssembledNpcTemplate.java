package com.aionemu.gameserver.model.templates.assemblednpc;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 组装 NPC 模板（静态数据/XML）。
 * Assembled NPC template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AssembledNpcTemplate")
public class AssembledNpcTemplate {
	@XmlAttribute(name = "nr")
	private int nr;

	@XmlAttribute(name = "routeId")
	private int routeId;

	@XmlAttribute(name = "mapId")
	private int mapId;

	@XmlAttribute(name = "liveTime")
	private int liveTime;

	@XmlElement(name = "assembled_part")
	private List<AssembledNpcPartTemplate> parts;

	/** 返回 nr / Returns the nr */
	public int getNr() {
		return nr;
	}

	/** 返回 route id / Returns the route id */
	public int getRouteId() {
		return routeId;
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return mapId;
	}

	/** 返回 live time / Returns the live time */
	public int getLiveTime() {
		return liveTime;
	}

	/** 返回 assembled npc part templates / Returns the assembled npc part templates */
	public List<AssembledNpcPartTemplate> getAssembledNpcPartTemplates() {
		return parts;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "AssembledNpcPart")
	public static class AssembledNpcPartTemplate {

		@XmlAttribute(name = "npcId")
		private int npcId;

		@XmlAttribute(name = "entityId")
		private int entityId;

		/** 返回 NPC ID / Returns the npc id */
		public int getNpcId() {
			return npcId;
		}

		/** 返回 entity id / Returns the entity id */
		public int getEntityId() {
			return entityId;
		}
	}
}
