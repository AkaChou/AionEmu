package com.aionemu.gameserver.model.templates.spawns;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.Setter;

/**
 * 房屋刷新模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "spawns" })
@XmlRootElement(name = "house")
public class HouseSpawns implements Comparable<HouseSpawns> {

	@XmlElement(name = "spawn", required = true)
	protected List<HouseSpawn> spawns;

	/** 返回地址 / Returns the address */
	@Getter
	@Setter
	@XmlAttribute(name = "address", required = true)
	protected int address;

	/** 获取刷新。 / Returns the spawns. */
	public List<HouseSpawn> getSpawns() {
		if (spawns == null) {
			spawns = new ArrayList<>();
		}
		return spawns;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(HouseSpawns o) {
		return o.address - address;
	}
}
