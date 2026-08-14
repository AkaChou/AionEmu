package com.aionemu.gameserver.model.templates.world;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * WeatherTable 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeatherTable", propOrder = { "zoneData" })
public class WeatherTable {
	@XmlElement(name = "table", required = true)
	protected List<WeatherEntry> zoneData;

	@XmlAttribute(name = "weather_count", required = true)
	protected int weatherCount;

	@XmlAttribute(name = "zone_count", required = true)
	protected int zoneCount;

	@XmlAttribute(name = "id", required = true)
	protected int mapId;

	/** 获取区域数据。 / Returns the zone data. */
	public List<WeatherEntry> getZoneData() {
		return zoneData;
	}

	/** 返回映射 ID / Returns the map id */
	public int getMapId() {
		return mapId;
	}

	/** 获取区域计数。 / Returns the zone count. */
	public int getZoneCount() {
		return zoneCount;
	}

	/** 返回天气数量 / Returns the weather count */
	public int getWeatherCount() {
		return weatherCount;
	}

	/** 返回下一天气条目 / Returns the weather after */
	public WeatherEntry getWeatherAfter(WeatherEntry entry) {
		if (entry.getWeatherName() == null || entry.isAfter()) {
			return null;
		}
		for (WeatherEntry we : getZoneData()) {
			if (we.getZoneId() != entry.getZoneId()) {
				continue;
			}
			if (entry.getWeatherName().equals(we.getWeatherName())) {
				if (entry.isBefore() && !we.isBefore() && !we.isAfter()) {
					return we;
				} else if (!entry.isBefore() && !entry.isAfter() && we.isAfter()) {
					return we;
				}
			}
		}
		return null;
	}

	/** 返回区域天气列表 / Returns the weathers for a zone */
	public List<WeatherEntry> getWeathersForZone(int zoneId) {
		List<WeatherEntry> result = new ArrayList<WeatherEntry>();
		for (WeatherEntry entry : getZoneData()) {
			if (entry.getZoneId() == zoneId) {
				result.add(entry);
			}
		}
		return result;
	}
}
