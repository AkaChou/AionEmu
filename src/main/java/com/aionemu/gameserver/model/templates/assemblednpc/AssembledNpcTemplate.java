package com.aionemu.gameserver.model.templates.assemblednpc;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * 组装 NPC 模板（静态数据/XML）。
 * Assembled NPC template (static data/XML).
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AssembledNpcTemplate")
public class AssembledNpcTemplate {
	/**
	 * -- GETTER --
	 * 返回 nr / Returns the nr
	 */
	@Getter
	@XmlAttribute(name = "nr")
	private int nr;

	/**
	 * -- GETTER --
	 * 返回 route id / Returns the route id
	 */
	@Getter
	@XmlAttribute(name = "routeId")
	private int routeId;

	/**
	 * -- GETTER --
	 * 返回映射 ID / Returns the map id
	 */
	@Getter
	@XmlAttribute(name = "mapId")
	private int mapId;

	/**
	 * -- GETTER --
	 * 返回 live time / Returns the live time
	 */
	@Getter
	@XmlAttribute(name = "liveTime")
	private int liveTime;

	@XmlElement(name = "assembled_part")
	private List<AssembledNpcPartTemplate> parts;

	/** 返回 assembled npc part templates / Returns the assembled npc part templates */
	public List<AssembledNpcPartTemplate> getAssembledNpcPartTemplates() {
		return parts;
	}

	@Getter
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "AssembledNpcPart")
	public static class AssembledNpcPartTemplate {

		/**
		 * -- GETTER --
		 * 返回 NPC ID / Returns the npc id
		 */
		@XmlAttribute(name = "npcId")
		private int npcId;

		/**
		 * -- GETTER --
		 * 返回 entity id / Returns the entity id
		 */
		@XmlAttribute(name = "entityId")
		private int entityId;

	}
}
