package com.aionemu.gameserver.model.templates.shield;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;

/**
 * 护盾模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author M@xx, Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Shield")
public class ShieldTemplate {

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "map")
	protected int map;

	@XmlAttribute(name = "id")
	protected int id;

	@XmlAttribute(name = "radius")
	protected float radius;

	@XmlElement(name = "center")
	protected ShieldPoint center;

	/** 获取名称。 / Returns the name. */
	public String getName() {
		return name;
	}

	/** 获取地图。 / Returns the map. */
	public int getMap() {
		return map;
	}

	/** 获取半径。 / Returns the radius. */
	public float getRadius() {
		return radius;
	}

	/** 返回居中 / Returns the center*/
	public ShieldPoint getCenter() {
		return center;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	public ShieldTemplate() {
	};

	public ShieldTemplate(String name, int mapId, Point3D center) {
		this.name = name;
		this.map = mapId;
		this.radius = 6;
		this.center = new ShieldPoint(center);
	}
}
