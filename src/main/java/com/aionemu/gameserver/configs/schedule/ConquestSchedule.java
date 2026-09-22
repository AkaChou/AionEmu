package com.aionemu.gameserver.configs.schedule;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.commons.utils.xml.JAXBUtil;
import com.aionemu.gameserver.configs.Config;
import lombok.Getter;
import lombok.Setter;

/**
 * Conquest 征服活动时间表配置。
 * Conquest event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@Getter
@XmlRootElement(name = "conquest_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConquestSchedule {
	/**
	 * Conquest 列表。
	 * List of conquests.
	 * -- GETTER --
	 *  获取 Conquest 列表。
	 *  Returns the conquest list.

	 */
	@XmlElement(name = "conquest", required = true)
	private List<Conquest> conquestsList;

	/**
	 * 设置 Conquest 列表。
	 * Sets the conquest list.
	 */
	public void setOfferingList(List<Conquest> conquestList) {
		this.conquestsList = conquestList;
	}

	/**
	 * 从 XML 加载时间表。
	 * Loads the schedule from XML.
	 */
	public static ConquestSchedule load() {
		ConquestSchedule cs;
		try {
			String xml = Files.readString(Config.configFile("schedule/conquest_schedule.xml").toPath(), StandardCharsets.UTF_8);
			cs = JAXBUtil.deserialize(xml, ConquestSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize conquest", e);
		}
		return cs;
	}

	/**
	 * 单个 Conquest 的时间表条目。
	 * Schedule entry for a single conquest.
	 */
	@Setter
	@Getter
    @XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "conquest")
	public static class Conquest {
		/**
		 * 征服 ID / Conquest ID
         * -- GETTER --
         *  获取 Conquest ID。
         *  Returns the conquest ID.
		 * -- SETTER --
		 *  设置 Conquest ID。
		 *  Sets the conquest ID.


		 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 献祭时间列表。
		 * List of offering times.
         * -- GETTER --
         *  获取献祭时间列表。
         *  Returns the offering times.
		 * -- SETTER --
		 *  设置献祭时间列表。
		 *  Sets the offering times.


		 */
		@XmlElement(name = "offeringTime", required = true)
		private List<String> offeringTimes;

	}
}
