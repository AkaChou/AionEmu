package com.aionemu.gameserver.model.templates.staticdoor;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 静态门世界模板（静态数据/XML）。
 * XML template.
 *
 * @author xTz
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "World")
public class StaticDoorWorld {

	@XmlAttribute(name = "world")
	protected int world;
	@XmlElement(name = "staticdoor")
	protected List<StaticDoorTemplate> staticDoorTemplate;

	/**
	 * 返回世界 ID。
	 * Returns the world id.
	 *
	 * @return 世界 ID / the world id
	 */
	public int getWorld() {
		return world;
	}

	/**
	 * 返回该世界的静态门模板列表。
	 * Returns the static door templates of this world.
	 *
	 * @return 静态门模板列表 / the list of static door templates
	 */
	public List<StaticDoorTemplate> getStaticDoors() {
		return staticDoorTemplate;
	}
}
