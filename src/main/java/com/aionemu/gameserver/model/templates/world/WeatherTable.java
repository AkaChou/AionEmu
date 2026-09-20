package com.aionemu.gameserver.model.templates.world;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;

/**
 * WeatherTable 模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeatherTable", propOrder = { "zoneData" })
public class WeatherTable {
	/**
	 * 区域天气条目；天气表允许为空（{@code weather_count="0"} 且无 {@code <table>} 子节点）时
	 * JAXB 不会写入该字段，故必须初始化为空列表，否则 {@link #getWeathersForZone(int)} 与
	 * {@link #getWeatherAfter(WeatherEntry)} 会空指针。
	 *
	 * Zone weather entries; when a table is empty (no {@code <table>} child) JAXB leaves this field
	 * untouched, so it must start as an empty list to keep the lookup methods null-safe.
	 */
	@XmlElement(name = "table", required = true)
	protected List<WeatherEntry> zoneData = new ArrayList<WeatherEntry>();

	/** 返回天气数量 / Returns the weather count */
	@XmlAttribute(name = "weather_count", required = true)
	protected int weatherCount;

	/** 获取区域计数。 / Returns the zone count. */
	@XmlAttribute(name = "zone_count", required = true)
	protected int zoneCount;

	/** 返回映射 ID / Returns the map id */
	@XmlAttribute(name = "id", required = true)
	protected int mapId;

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
