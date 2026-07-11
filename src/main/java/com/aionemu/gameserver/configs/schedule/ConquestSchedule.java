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

/**
 * Conquest 征服活动时间表配置。
 * Conquest event schedule configuration.
 *
 * @author Rinzler (Encom)
 */
@XmlRootElement(name = "conquest_schedule")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConquestSchedule {
	/**
	 * Conquest 列表。
	 * List of conquests.
	 */
	@XmlElement(name = "conquest", required = true)
	private List<Conquest> conquestsList;

	/**
	 * 获取 Conquest 列表。
	 * Returns the conquest list.
	 */
	public List<Conquest> getConquestsList() {
		return conquestsList;
	}

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
			cs = (ConquestSchedule) JAXBUtil.deserialize(xml, ConquestSchedule.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize conquest", e);
		}
		return cs;
	}

	/**
	 * 单个 Conquest 的时间表条目。
	 * Schedule entry for a single conquest.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "conquest")
	public static class Conquest {
		/**
	 * 征服 ID / Conquest ID
	 */
		@XmlAttribute(required = true)
		private int id;

		/**
		 * 献祭时间列表。
		 * List of offering times.
		 */
		@XmlElement(name = "offeringTime", required = true)
		private List<String> offeringTimes;

		/**
		 * 获取 Conquest ID。
		 * Returns the conquest ID.
		 */
		public int getId() {
			return id;
		}

		/**
		 * 设置 Conquest ID。
		 * Sets the conquest ID.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * 获取献祭时间列表。
		 * Returns the offering times.
		 */
		public List<String> getOfferingTimes() {
			return offeringTimes;
		}

		/**
		 * 设置献祭时间列表。
		 * Sets the offering times.
		 */
		public void setOfferingTimes(List<String> offeringTimes) {
			this.offeringTimes = offeringTimes;
		}
	}
}
