package com.aionemu.gameserver.model.templates.zone;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.world.zone.ZoneName;
import lombok.Getter;

/**
 * 区域模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "Zone")
public class ZoneTemplate {

	/**
	 * 获取 points 属性值。
	 * Gets the value of the points property.
	 */
	@Getter
	@XmlElement
	protected Points points;

	/** 获取圆柱。 / Returns the cylinder. */
	@Getter
	@XmlElement
	protected Cylinder cylinder;

	/** 获取球体。 / Returns the sphere. */
	@Getter
	@XmlElement
	protected Sphere sphere;

	/** 获取半球。 / Returns the semisphere. */
	@Getter
	@XmlElement
	protected Semisphere semisphere;

	/** 返回 flags / Returns the flags */
	@Getter
	@XmlAttribute
	protected int flags = -1;

	/**
	 * @return the priority
	 */
	@Getter
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

	/**
	 * 获取 mapid 属性值。
	 * Gets the value of the mapid property.
	 */
	@Getter
	@XmlAttribute
	protected int mapid;

	/** 返回攻城 ID / Returns the siege id */
	@Getter
	@XmlAttribute(name = "siege_id")
	protected List<Integer> siegeId;

	/** 返回城镇 ID / Returns the town id */
	@Getter
	@XmlAttribute(name = "town_id")
	private int townId;

	/**
	 * @return the type
	 */
	@Getter
	@XmlAttribute(name = "area_type")
	protected AreaType areaType = AreaType.POLYGON;

	/**
	 * @return the zoneType
	 */
	@Getter
	@XmlAttribute(name = "zone_type")
	protected ZoneClassName zoneType = ZoneClassName.SUB;

	/**
	 * 获取区域名称属性值。
	 * Gets the value of the name property.
	 */
	public ZoneName getName() {
		return zoneName;
	}
}
