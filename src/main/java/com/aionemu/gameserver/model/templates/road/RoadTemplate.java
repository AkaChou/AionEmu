package com.aionemu.gameserver.model.templates.road;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;

/**
 * 道路模板（静态数据/XML）。
 * XML template.
 *
 * @author SheppeR
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Road")
public class RoadTemplate {

	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "map")
	protected int map;

	@XmlAttribute(name = "radius")
	protected float radius;

	@XmlElement(name = "center")
	protected RoadPoint center;

	@XmlElement(name = "p1")
	protected RoadPoint p1;

	@XmlElement(name = "p2")
	protected RoadPoint p2;

	@XmlElement(name = "roadexit")
	protected RoadExit roadExit;

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

	/** 返回居中 / Returns the center. */
	public RoadPoint getCenter() {
		return center;
	}

	/** 返回 p 1 / Returns the p 1 */
	public RoadPoint getP1() {
		return p1;
	}

	/** 返回 p 2 / Returns the p 2 */
	public RoadPoint getP2() {
		return p2;
	}

	/** 返回 road exit / Returns the road exit */
	public RoadExit getRoadExit() {
		return roadExit;
	}

	public RoadTemplate() {

	};

	public RoadTemplate(String name, int mapId, Point3D center, Point3D p1, Point3D p2) {
		this.name = name;
		this.map = mapId;
		this.radius = 6;
		this.center = new RoadPoint(center);
		this.p1 = new RoadPoint(p1);
		this.p2 = new RoadPoint(p2);
	}
}
