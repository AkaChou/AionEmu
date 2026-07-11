package com.aionemu.gameserver.dataholders;

import java.util.List;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.world.WeatherTable;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 地图天气数据容器，按地图 ID 索引 {@link WeatherTable}。
 * Map weather data holder, indexing {@link WeatherTable} by map id.
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "weatherData" })
@XmlRootElement(name = "weather")
public class MapWeatherData {

	@XmlElement(name = "map", required = true)
	private List<WeatherTable> weatherData;
	@XmlTransient
	private IntObjectHashMap<WeatherTable> mapWeather;

	/**
	 * JAXB 反序列化完成后，按地图 ID 建立天气表索引并释放列表。
	 * After JAXB unmarshalling, indexes weather tables by map id and clears the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		mapWeather = new IntObjectHashMap<WeatherTable>();

		for (WeatherTable table : weatherData) {
			mapWeather.put(table.getMapId(), table);
		}
		weatherData.clear();
		weatherData = null;
	}

	/**
	 * 按地图 ID 获取天气表。
	 * Returns the weather table for the given map id.
	 *
	 * map id
	 * weather table or null
	 */
	public WeatherTable getWeather(int mapId) {
		return mapWeather.get(mapId);
	}

	/**
	 * 返回已加载的地图天气表数量。
	 * Returns the number of loaded map weather tables.
	 *
	 * @return 天气表数量 / weather table count
	 */
	public int size() {
		return mapWeather.size();
	}
}
