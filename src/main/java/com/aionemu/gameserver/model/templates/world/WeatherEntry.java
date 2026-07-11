package com.aionemu.gameserver.model.templates.world;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Weather 条目模板（静态数据/XML）。
 * XML template. / XML template.
 *
 * @author Rinzler (Encom)
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeatherEntry")
public class WeatherEntry {
	public WeatherEntry() {
	}

	public WeatherEntry(int zoneId, int weatherCode) {
		this.weatherCode = weatherCode;
		this.zoneId = zoneId;
	}

	@XmlAttribute(name = "zone_id", required = true)
	private int zoneId;

	@XmlAttribute(name = "code", required = true)
	private int weatherCode;

	@XmlAttribute(name = "att_ranking", required = true)
	private int attRanking;

	@XmlAttribute(name = "name")
	private String weatherName;

	@XmlAttribute(name = "before")
	private Boolean isBefore;

	@XmlAttribute(name = "after")
	private Boolean isAfter;

	/** 返回区域 ID / Returns the zone id */
	public int getZoneId() {
		return zoneId;
	}

	/** 返回 code / Returns the code */
	public int getCode() {
		return weatherCode;
	}

	/** 返回 att ranking / Returns the att ranking */
	public int getAttRanking() {
		return attRanking;
	}

	/** Whether 前 / Whether before */
	public Boolean isBefore() {
		if (isBefore == null) {
			return false;
		}
		return isBefore;
	}

	/** 是否后 / Whether after*/
	public Boolean isAfter() {
		if (isAfter == null) {
			return false;
		}
		return isAfter;
	}

	/** 返回 weather name / Returns the weather name */
	public String getWeatherName() {
		return weatherName;
	}
}
