package com.aionemu.gameserver.model.templates.staticdoor;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 静态 Door 世界模板（静态数据/XML）。
 * XML template. / XML template.
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
	 * @return the world
	 */
	public int getWorld() {
		return world;
	}

	/**
	 * @return the List<StaticDoorTemplate>
	 */
	public List<StaticDoorTemplate> getStaticDoors() {
		return staticDoorTemplate;
	}
}
