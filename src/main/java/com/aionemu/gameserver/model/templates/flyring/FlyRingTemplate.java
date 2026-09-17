package com.aionemu.gameserver.model.templates.flyring;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 飞行光环模板（静态数据/XML）。
 * XML template.
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FlyRing")
@NoArgsConstructor
public class FlyRingTemplate {
	/** 获取名称。 / Returns the name. */
	@XmlAttribute(name = "name")
	protected String name;

	/** 获取地图。 / Returns the map. */
	@XmlAttribute(name = "map")
	protected int map;

	/** 获取半径。 / Returns the radius. */
	@XmlAttribute(name = "radius")
	protected float radius;

	/** 返回中心点 / Returns the center*/
	@XmlElement(name = "center")
	protected FlyRingPoint center;

	/** 返回左点 / Returns the left */
	@XmlElement(name = "left")
	protected FlyRingPoint left;

	/** 返回右点 / Returns the right */
	@XmlElement(name = "right")
	protected FlyRingPoint right;

	public FlyRingTemplate(String name, int mapId, Point3D center, Point3D left, Point3D right, int radius) {
		this.name = name;
		this.map = mapId;
		this.radius = radius;
		this.center = new FlyRingPoint(center);
		this.left = new FlyRingPoint(left);
		this.right = new FlyRingPoint(right);
	}
}
