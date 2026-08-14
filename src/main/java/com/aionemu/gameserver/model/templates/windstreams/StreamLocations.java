package com.aionemu.gameserver.model.templates.windstreams;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 风道地点集模板（静态数据/XML）。
 * Stream locations template (static data/XML).
 *
 * @author LokiReborn
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StreamLocations")
public class StreamLocations {
	@XmlElement(required = true)
	protected List<Location2D> location;

	public StreamLocations() {
	}

	public StreamLocations(List<Location2D> location) {
		this.location = location;
	}

	/** 获取位置。 / Returns the location. */
	public List<Location2D> getLocation() {

		if (location == null) {
			location = new ArrayList<>();
		}
		return this.location;
	}
}
