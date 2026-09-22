package com.aionemu.gameserver.model.templates.windstreams;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 风道模板（静态数据/XML）。
 * Windstream template (static data/XML).
 * @author LokiReborn
 */
@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WindFlight")
@NoArgsConstructor
public class WindstreamTemplate {
	/**
	 * 获取 locations 属性值。
	 * Gets the value of the locations property
	 */
	@XmlElement(required = true)
	protected StreamLocations locations;
	/**
	 * 获取 mapid 属性值。
	 * Gets the value of the mapid property
	 */
	@XmlAttribute
	protected int mapid;

	public WindstreamTemplate(int mapId, List<Location2D> locations) {
		this.mapid = mapId;
		this.locations = new StreamLocations(locations);
	}
}
