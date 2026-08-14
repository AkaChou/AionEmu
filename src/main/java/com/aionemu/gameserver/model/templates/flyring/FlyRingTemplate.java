package com.aionemu.gameserver.model.templates.flyring;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;

/**
 * 飞行光环模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FlyRing")
public class FlyRingTemplate {
	@XmlAttribute(name = "name")
	protected String name;

	@XmlAttribute(name = "map")
	protected int map;

	@XmlAttribute(name = "radius")
	protected float radius;

	@XmlElement(name = "center")
	protected FlyRingPoint center;

	@XmlElement(name = "left")
	protected FlyRingPoint left;

	@XmlElement(name = "right")
	protected FlyRingPoint right;

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

	/** 返回中心点 / Returns the center*/
	public FlyRingPoint getCenter() {
		return center;
	}

	/** 返回左点 / Returns the left */
	public FlyRingPoint getLeft() {
		return left;
	}

	/** 返回右点 / Returns the right */
	public FlyRingPoint getRight() {
		return right;
	}

	public FlyRingTemplate() {
	};

	public FlyRingTemplate(String name, int mapId, Point3D center, Point3D left, Point3D right, int radius) {
		this.name = name;
		this.map = mapId;
		this.radius = radius;
		this.center = new FlyRingPoint(center);
		this.left = new FlyRingPoint(left);
		this.right = new FlyRingPoint(right);
	}
}
