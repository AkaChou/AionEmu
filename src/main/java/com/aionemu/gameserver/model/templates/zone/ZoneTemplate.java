package com.aionemu.gameserver.model.templates.zone;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * 区域模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Zone")
public class ZoneTemplate {

	@XmlElement
	protected Points points;

	@XmlElement
	protected Cylinder cylinder;

	@XmlElement
	protected Sphere sphere;

	@XmlElement
	protected Semisphere semisphere;

	@XmlAttribute
	protected int flags = -1;

	@XmlAttribute
	protected int priority;

	@XmlTransient
	private String name;

	@XmlTransient
	private ZoneName zoneName;

	@XmlAttribute(name = "name")
	/** 返回 xml name / Returns the xml name */
	public String getXmlName() {
		return name;
	}

	protected void setXmlName(String name) {
		zoneName = ZoneName.createOrGet(name);
		this.name = zoneName.name();
	}

	@XmlAttribute
	protected int mapid;

	@XmlAttribute(name = "siege_id")
	protected List<Integer> siegeId;

	@XmlAttribute(name = "town_id")
	private int townId;

	@XmlAttribute(name = "area_type")
	protected AreaType areaType = AreaType.POLYGON;

	@XmlAttribute(name = "zone_type")
	protected ZoneClassName zoneType = ZoneClassName.SUB;

	/**
	 * 获取 points 属性值。
	 * Gets the value of the points property.
	 */
	public Points getPoints() {
		return points;
	}

	/** 获取圆柱。 / Returns the cylinder. */
	public Cylinder getCylinder() {
		return cylinder;
	}

	/** 获取球体。 / Returns the sphere. */
	public Sphere getSphere() {
		return sphere;
	}

	/** 获取半球。 / Returns the semisphere. */
	public Semisphere getSemisphere() {
		return semisphere;
	}

	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * 获取区域名称属性值。
	 * Gets the value of the name property.
	 */
	public ZoneName getName() {
		return zoneName;
	}

	/**
	 * 获取 mapid 属性值。
	 * Gets the value of the mapid property.
	 */
	public int getMapid() {
		return mapid;
	}

	/**
	 * @return the type
	 */
	public AreaType getAreaType() {
		return areaType;
	}

	/**
	 * @return the zoneType
	 */
	public ZoneClassName getZoneType() {
		return zoneType;
	}

	/** 返回攻城 ID / Returns the siege id */
	public List<Integer> getSiegeId() {
		return siegeId;
	}

	/** 返回 flags / Returns the flags */
	public int getFlags() {
		return flags;
	}

	/** 返回城镇 ID / Returns the town id */
	public int getTownId() {
		return townId;
	}
}
