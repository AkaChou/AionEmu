package com.aionemu.gameserver.model.templates.towns;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 城镇刷新点模板（静态数据/XML）。
 * XML template.
 *
 * @author ViAl
 */
@XmlType(name = "town_spawn")
public class TownSpawn {

	@XmlAttribute(name = "town_id")
	private int townId;
	@XmlElement(name = "town_level")
	private List<TownLevel> townLevels;
	private Map<Integer, TownLevel> townLevelsData = new HashMap<Integer, TownLevel>();

	/**
	 * @param u
	 * @param parent
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		townLevelsData.clear();

		for (TownLevel level : townLevels) {
			townLevelsData.put(level.getLevel(), level);
		}
		townLevels.clear();
		townLevels = null;
	}

	/**
	 * @return the townId
	 */
	public int getTownId() {
		return townId;
	}

	/** 返回 spawns for level / Returns the spawns for level */
	public TownLevel getSpawnsForLevel(int level) {
		return townLevelsData.get(level);
	}

	/** 返回 town levels / Returns the town levels */
	public Collection<TownLevel> getTownLevels() {
		return this.townLevelsData.values();
	}
}
