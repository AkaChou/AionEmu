package com.aionemu.gameserver.questEngine.handlers.models;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import lombok.Getter;

/**
 * 任务脚本中引用的世界地图条目。
 * World-map entry referenced by quest scripts.
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WorldData")
public class WorldData {

	/**
	 * 世界地图 ID（mapId）。
	 * World map id (mapId).
	 */
	@XmlAttribute(name = "id", required = true)
	protected int worldId;
}
