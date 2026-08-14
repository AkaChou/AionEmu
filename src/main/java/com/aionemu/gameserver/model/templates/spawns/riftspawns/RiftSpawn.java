package com.aionemu.gameserver.model.templates.spawns.riftspawns;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.spawns.Spawn;

/**
 * 裂隙刷新点模板（静态数据/XML）。
 * Rift spawn template (static data / XML).
 *
 * @author Source
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RiftSpawn")
public class RiftSpawn {

	@XmlAttribute(name = "id")
	private int id;
	@XmlAttribute(name = "world")
	private int world;
	@XmlElement(name = "spawn")
	private List<Spawn> spawns = new ArrayList<Spawn>();

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回世界 ID / Returns the world id */
	public int getWorldId() {
		return world;
	}

	/** 获取刷新。 / Returns the spawns. */
	public List<Spawn> getSpawns() {
		return spawns;
	}
}
