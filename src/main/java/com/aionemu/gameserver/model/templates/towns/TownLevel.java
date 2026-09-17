package com.aionemu.gameserver.model.templates.towns;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.spawns.Spawn;
import lombok.Getter;

/**
 * 城镇等级模板（静态数据/XML）。
 * XML template.
 *
 * @author ViAl
 */
@Getter
@XmlType(name = "town_level")
public class TownLevel {

	/**
	 * @return the level
	 */
	@XmlAttribute(name = "level")
	protected int level;
	/**
	 * @return the spawn
	 */
	@XmlElement(name = "spawn")
	protected List<Spawn> spawns;
}
