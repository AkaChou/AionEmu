package com.aionemu.gameserver.model.templates.shield;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.utils3d.Point3D;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 护盾模板（静态数据/XML）。
 * XML template.
 *
 * @author M@xx, Wakizashi
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Shield")
@NoArgsConstructor
public class ShieldTemplate {

	/** 获取名称。 / Returns the name. */
	@Getter
	@XmlAttribute(name = "name")
	protected String name;

	/** 获取地图。 / Returns the map. */
	@Getter
	@XmlAttribute(name = "map")
	protected int map;

	/** 返回 ID / Returns the id */
	@Getter
	@XmlAttribute(name = "id")
	protected int id;

	/** 获取半径。 / Returns the radius. */
	@Getter
	@XmlAttribute(name = "radius")
	protected float radius;

	/** 返回中心点 / Returns the center */
	@Getter
	@XmlElement(name = "center")
	protected ShieldPoint center;

	/**
	 * 用给定名称、地图和中心点构造护盾模板，默认半径 6。
	 * Constructs a shield template with the given name, map and center; default radius is 6.
	 *
	 * @param name 护盾名称 / shield name
	 * @param mapId 地图 ID / map id
	 * @param center 中心点 / center point
	 */
	public ShieldTemplate(String name, int mapId, Point3D center) {
		this.name = name;
		this.map = mapId;
		this.radius = 6;
		this.center = new ShieldPoint(center);
	}
}
