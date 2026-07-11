package com.aionemu.gameserver.model.templates.spawns;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.event.EventTemplate;
import com.aionemu.gameserver.spawnengine.SpawnHandlerType;

/**
 * 刷新点模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Spawn")
public class Spawn {
	@XmlAttribute(name = "custom")
	private Boolean isCustom = false;

	@XmlAttribute(name = "handler")
	private SpawnHandlerType handler;

	@XmlAttribute(name = "pool")
	private Integer pool = 0;

	@XmlAttribute(name = "respawn_time")
	private Integer respawnTime = 0;

	@XmlAttribute(name = "npc_id", required = true)
	private int npcId;

	@XmlAttribute(name = "difficult_id")
	private byte difficultId;

	@XmlElement(name = "temporary_spawn")
	private TemporarySpawn temporaySpawn;

	@XmlElement(name = "spot")
	private List<SpawnSpotTemplate> spawnTemplates;

	@XmlTransient
	private EventTemplate eventTemplate;

	public Spawn() {
	}

	public Spawn(int npcId, int respawnTime, SpawnHandlerType handler) {
		this.npcId = npcId;
		this.respawnTime = respawnTime;
		this.handler = handler;
	}

	void beforeMarshal(Marshaller marshaller) {
		if (pool == 0) {
			pool = null;
		}
		if (isCustom == false) {
			isCustom = null;
		}
	}

	void afterMarshal(Marshaller marshaller) {
		if (isCustom == null) {
			isCustom = false;
		}
		if (pool == null) {
			pool = 0;
		}
	}

	/** 返回 NPC ID / Returns the npc id */
	public int getNpcId() {
		return npcId;
	}

	/** 返回 pool / Returns the pool */
	public int getPool() {
		return pool;
	}

	/** 返回临时刷新 / Returns the temporary spawn*/
	public TemporarySpawn getTemporarySpawn() {
		return temporaySpawn;
	}

	/** 返回 respawn time / Returns the respawn time */
	public int getRespawnTime() {
		return respawnTime;
	}

	/** 获取刷新点处理器类型。 / Returns the spawn handler type. */
	public SpawnHandlerType getSpawnHandlerType() {
		return handler;
	}

	/** 返回 spawn spot templates / Returns the spawn spot templates */
	public List<SpawnSpotTemplate> getSpawnSpotTemplates() {
		if (spawnTemplates == null) {
			spawnTemplates = new ArrayList<SpawnSpotTemplate>();
		}
		return spawnTemplates;
	}

	/** 添加 spawn spot / Adds spawn spot */
	public void addSpawnSpot(SpawnSpotTemplate template) {
		getSpawnSpotTemplates().add(template);
	}

	/** 是否为自定义。 / Whether custom. */
	public boolean isCustom() {
		return isCustom == null ? false : isCustom;
	}

	/** 设置自定义。 / Sets the custom. */
	public void setCustom(boolean isCustom) {
		this.isCustom = isCustom;
	}

	/** 是否为活动刷新点。 / Whether event spawn. */
	public boolean isEventSpawn() {
		return eventTemplate != null;
	}

	/** 获取活动模板。 / Returns the event template. */
	public EventTemplate getEventTemplate() {
		return eventTemplate;
	}

	/** 设置活动模板。 / Sets the event template. */
	public void setEventTemplate(EventTemplate eventTemplate) {
		this.eventTemplate = eventTemplate;
	}

	/** 返回 difficult id / Returns the difficult id */
	public byte getDifficultId() {
		return difficultId;
	}
}
