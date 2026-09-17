package com.aionemu.gameserver.model.templates.world;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Weather 条目模板（静态数据/XML）。
 * XML template.
 *
 * @author Rinzler (Encom)
 */

@Getter
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WeatherEntry")
@NoArgsConstructor
public class WeatherEntry {
	public WeatherEntry(int zoneId, int weatherCode) {
		this.weatherCode = weatherCode;
		this.zoneId = zoneId;
	}

	/** 返回区域 ID / Returns the zone id */
	@XmlAttribute(name = "zone_id", required = true)
	private int zoneId;

	@XmlAttribute(name = "code", required = true)
	private int weatherCode;

	/** 返回占领排名 / Returns the att ranking */
	@XmlAttribute(name = "att_ranking", required = true)
	private int attRanking;

	/** 返回 weather name / Returns the weather name */
	@XmlAttribute(name = "name")
	private String weatherName;

	@XmlAttribute(name = "before")
	private Boolean isBefore;

	@XmlAttribute(name = "after")
	private Boolean isAfter;

	/** 返回天气代码 / Returns the code */
	public int getCode() {
		return weatherCode;
	}

	/** 是否为前序天气 / Whether before */
	public Boolean isBefore() {
		if (isBefore == null) {
			return false;
		}
		return isBefore;
	}

	/** 是否为后续天气 / Whether after. */
	public Boolean isAfter() {
		if (isAfter == null) {
			return false;
		}
		return isAfter;
	}
}
